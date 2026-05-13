"""서버 end-to-end 통합 테스트.

흐름:
    1) /health 로 서버·Ollama 연결 확인
    2) /auth/register 로 테스트 유저 가입 (이미 있으면 skip)
    3) /auth/login 으로 액세스 토큰 발급
    4) data/contract_ex1.txt 읽어 /contract/analyze-text 로 전송
    5) 서버가 LLM에 분석을 위임하고 받아온 결과를 출력

사용:
    # 1) 별도 터미널에서 서버 기동
    cd server && uvicorn main:app --reload --port 8000

    # 2) 본 스크립트 실행
    python3 server_test.py
    python3 server_test.py --base-url http://localhost:8000 --save-json out.json
"""
from __future__ import annotations

import argparse
import asyncio
import json
import sys
from pathlib import Path

import httpx


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONTRACT_PATH = REPO_ROOT / "data" / "contract_ex1.txt"
DEFAULT_BASE_URL = "http://localhost:8000"
DEFAULT_USERNAME = "test_user"
DEFAULT_EMAIL = "test@example.com"
DEFAULT_PASSWORD = "test_password_123"


async def ensure_user(
    client: httpx.AsyncClient, username: str, email: str, password: str
) -> None:
    """가입 시도. 이미 등록돼 있으면 그대로 진행."""
    r = await client.post(
        "/auth/register",
        json={"username": username, "email": email, "password": password},
    )
    if r.status_code == 201:
        print(f"✅ 유저 가입 완료: {username}")
    elif r.status_code == 400:
        # 이미 사용 중인 이메일/사용자명
        print(f"ℹ️  기존 유저 재사용: {username}")
    else:
        print(f"❌ 가입 실패 ({r.status_code}): {r.text}", file=sys.stderr)
        r.raise_for_status()


async def login(client: httpx.AsyncClient, username: str, password: str) -> str:
    r = await client.post(
        "/auth/login",
        data={"username": username, "password": password},
    )
    r.raise_for_status()
    token = r.json()["access_token"]
    print("🔑 로그인 토큰 발급")
    return token


async def analyze_text(client: httpx.AsyncClient, token: str, text: str) -> dict:
    r = await client.post(
        "/contract/analyze-text",
        json={"text": text},
        headers={"Authorization": f"Bearer {token}"},
        timeout=600.0,  # LLM 호출이 길어질 수 있음
    )
    r.raise_for_status()
    return r.json()


def print_result(result: dict) -> None:
    bar = "=" * 70
    print(bar)
    print(f"분석 ID  : {result.get('id')}")
    print(f"위험도   : {result.get('risk_level')}")
    print(f"요약     : {result.get('summary')}")
    print(f"생성시각 : {result.get('created_at')}")
    print("-" * 70)

    toxic = result.get("toxic_clauses") or []
    if not toxic:
        print("독소조항: 없음")
    else:
        print(f"독소조항 {len(toxic)}개:")
        for i, c in enumerate(toxic, 1):
            print(f"\n  [{i}] 위험도={c.get('severity')}")
            print(f"      조항: {c.get('clause')}")
            print(f"      이유: {c.get('reason')}")
            print(f"      권장: {c.get('recommendation')}")
    print(bar)


async def main() -> int:
    parser = argparse.ArgumentParser(description="서버 통합 테스트")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL,
                        help=f"FastAPI 서버 베이스 URL (기본: {DEFAULT_BASE_URL})")
    parser.add_argument("--contract", type=Path, default=DEFAULT_CONTRACT_PATH,
                        help=f"분석할 계약서 텍스트 (기본: {DEFAULT_CONTRACT_PATH})")
    parser.add_argument("--username", default=DEFAULT_USERNAME)
    parser.add_argument("--email", default=DEFAULT_EMAIL)
    parser.add_argument("--password", default=DEFAULT_PASSWORD)
    parser.add_argument("--save-json", type=Path, default=None,
                        help="분석 결과를 저장할 JSON 경로")
    args = parser.parse_args()

    if not args.contract.exists():
        print(f"❌ 계약서 파일 없음: {args.contract}", file=sys.stderr)
        return 1

    async with httpx.AsyncClient(base_url=args.base_url, timeout=30.0) as client:
        # 1) 서버 헬스 체크
        try:
            r = await client.get("/health")
            r.raise_for_status()
            health = r.json()
        except httpx.HTTPError as e:
            print(f"❌ 서버 연결 실패 ({args.base_url}): {e}", file=sys.stderr)
            print("   서버 기동 확인: cd server && uvicorn main:app --port 8000",
                  file=sys.stderr)
            return 2
        print(f"🏥 서버 상태: {health}")
        if health.get("ollama") != "connected":
            print("⚠️  Ollama 미연결 — 분석 호출이 실패할 가능성이 큽니다.",
                  file=sys.stderr)

        # 2) 가입 → 로그인
        await ensure_user(client, args.username, args.email, args.password)
        token = await login(client, args.username, args.password)

        # 3) 계약서 텍스트 전송
        text = args.contract.read_text(encoding="utf-8")
        print(f"\n📄 계약서 전송: {args.contract} ({len(text):,}자)")
        print("⏳ LLM 분석 중... (수십 초~몇 분 걸릴 수 있음)")
        try:
            result = await analyze_text(client, token, text)
        except httpx.HTTPStatusError as e:
            print(f"❌ 분석 실패 ({e.response.status_code}): {e.response.text}",
                  file=sys.stderr)
            return 3

        print()
        print_result(result)

        if args.save_json:
            args.save_json.write_text(
                json.dumps(result, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(f"\n💾 결과 저장: {args.save_json}")

    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
