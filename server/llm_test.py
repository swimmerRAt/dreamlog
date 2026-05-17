"""독소조항 탐지 학습-검증 스크립트.

1) `data/contract_rule/` 폴더 안의 CSV 파일들을 참조 자료로 묶어 Ollama 커스텀
   모델을 재등록한다. (시스템 프롬프트에 라벨링된 조항·법조문이 주입된다.)
2) `data/contract_ex1.txt` 계약서를 그 모델로 분석해 독소조항 추출 결과를 출력한다.

사용 예:
    python3 llm_test.py
    python3 llm_test.py --rule ../data/contract_rule --contract ../data/contract_ex1.txt
    python3 llm_test.py --skip-train       # 기존 모델 재사용, 분석만 수행
    python3 llm_test.py --save-json out.json
"""
from __future__ import annotations

import argparse
import asyncio
import csv
import json
import sys
from pathlib import Path

from llm import (
    TOXIC_DETECTOR_MODEL,
    analyze_contract,
    create_toxic_detector,
    get_client,
)


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_RULE_PATH = REPO_ROOT / "data" / "contract_rule"
DEFAULT_CONTRACT_PATH = REPO_ROOT / "data" / "contract_ex1.txt"

# 학습에 사용할 CSV 파일명(확장자 제외). 폴더에 다른 파일이 있어도 무시되고,
# 여기 적힌 순서대로 참조 자료에 들어간다.
RULE_FILES = (
    "계약서_독소조항_데이터셋",
    "독소조항_수정안_페어",
    "주택임대차보호법_조문_위험도라벨",
)


def _row_to_text(headers: list[str], row: list[str]) -> str:
    parts: list[str] = []
    for h, v in zip(headers, row):
        sv = (v or "").strip()
        if not sv or sv == "—":
            continue
        parts.append(f"{h}: {sv}")
    return " | ".join(parts)


def _read_csv(path: Path) -> list[list[str]]:
    # utf-8-sig: Excel에서 저장한 BOM 있는 CSV도, 일반 utf-8 CSV도 모두 처리
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        return [row for row in csv.reader(f)]


def load_rule_references(path: Path) -> list[str]:
    """CSV 폴더 안의 각 파일을 한 덩어리 텍스트(독소조항 학습 자료)로 변환."""
    references: list[str] = []
    for name in RULE_FILES:
        csv_path = path / f"{name}.csv"
        if not csv_path.exists():
            continue
        rows = _read_csv(csv_path)
        if len(rows) < 2:
            continue
        headers = [(h or "").strip() for h in rows[0]]
        lines = [f"## {name}"]
        for r in rows[1:]:
            text = _row_to_text(headers, r)
            if text:
                lines.append(f"- {text}")
        if len(lines) > 1:
            references.append("\n".join(lines))
    return references


def print_result(contract_text: str, result: dict) -> None:
    bar = "=" * 70
    print(bar)
    print(f"위험도: {result.get('risk_level', '?')}")
    print(f"요약 : {result.get('summary', '')}")
    print("-" * 70)
    print(f"원문 (앞 200자): {contract_text[:200]}{'...' if len(contract_text) > 200 else ''}")
    print("-" * 70)

    toxic = result.get("toxic_clauses") or []
    if not toxic:
        print("독소조항: 없음")
    else:
        print(f"독소조항 {len(toxic)}개:")
        for i, clause in enumerate(toxic, 1):
            print(f"\n  [{i}] 위험도={clause.get('severity', '?')}")
            print(f"      조항: {clause.get('clause', '')}")
            print(f"      이유: {clause.get('reason', '')}")
            print(f"      권장: {clause.get('recommendation', '')}")

    if "error" in result:
        print(f"\n⚠️  오류: {result['error']}")
    print(bar)


async def main() -> int:
    parser = argparse.ArgumentParser(description="contract_rule/ CSV들로 학습 후 contract_ex1.txt 분석")
    parser.add_argument("--rule", type=Path, default=DEFAULT_RULE_PATH,
                        help=f"규칙 CSV 폴더 경로 (기본: {DEFAULT_RULE_PATH})")
    parser.add_argument("--contract", type=Path, default=DEFAULT_CONTRACT_PATH,
                        help=f"분석할 계약서 텍스트 경로 (기본: {DEFAULT_CONTRACT_PATH})")
    parser.add_argument("--skip-train", action="store_true",
                        help="모델 재등록을 건너뛰고 기존 모델로 분석만 수행")
    parser.add_argument("--save-json", type=Path, default=None,
                        help="분석 결과 JSON 저장 경로")
    parser.add_argument("--save-modelfile", type=Path, default=None,
                        help="생성된 Modelfile을 저장할 경로 (디버깅용)")
    args = parser.parse_args()

    if not args.rule.exists() or not args.rule.is_dir():
        print(f"❌ 규칙 폴더 없음 또는 폴더가 아님: {args.rule}", file=sys.stderr)
        return 1
    if not args.contract.exists():
        print(f"❌ 계약서 파일 없음: {args.contract}", file=sys.stderr)
        return 1

    cli = get_client()
    if not await cli.health():
        print("❌ Ollama 서버에 연결 불가. 'ollama serve'를 먼저 실행하세요.", file=sys.stderr)
        return 2

    if not args.skip_train:
        print(f"📚 규칙 학습 자료 로드: {args.rule}")
        references = load_rule_references(args.rule)
        total_chars = sum(len(r) for r in references)
        print(f"   {len(references)}개 CSV / 총 {total_chars:,}자")

        print(f"🛠️  커스텀 모델 등록 중: {TOXIC_DETECTOR_MODEL}")
        modelfile = await create_toxic_detector(extra_references=references)
        print(f"   완료 (Modelfile {len(modelfile):,}자)")
        if args.save_modelfile:
            args.save_modelfile.write_text(modelfile, encoding="utf-8")
            print(f"   Modelfile 저장: {args.save_modelfile}")
    else:
        if not await cli.has_model(TOXIC_DETECTOR_MODEL):
            print(f"⚠️  모델 '{TOXIC_DETECTOR_MODEL}'이 등록돼 있지 않습니다. --skip-train 없이 다시 실행하세요.",
                  file=sys.stderr)
            return 3
        print(f"⏭️  학습 단계 건너뜀. 기존 모델 '{TOXIC_DETECTOR_MODEL}' 사용")

    print(f"\n📄 계약서 분석: {args.contract}")
    contract_text = args.contract.read_text(encoding="utf-8")

    result = await analyze_contract(contract_text)
    print()
    print_result(contract_text, result)

    if args.save_json:
        args.save_json.write_text(
            json.dumps({"input": contract_text, "result": result}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        print(f"\n💾 결과 저장: {args.save_json}")

    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
