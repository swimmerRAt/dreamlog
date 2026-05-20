"""계약서 사진을 LLM으로 OCR해서 data/contract_exN.txt로 저장한다.

입력 이미지는 기본적으로 data/image/ 폴더에서 찾는다.
절대 경로나 data/image/ 외부 경로를 주면 그대로 사용한다.

사용 예:
    python3 ocr_image.py photo.jpg                 # data/image/photo.jpg 사용
    python3 ocr_image.py photo.jpg --index 5       # contract_ex5.txt로 강제 지정
    python3 ocr_image.py /abs/path/photo.jpg       # 절대 경로 그대로 사용
"""
from __future__ import annotations

import argparse
import asyncio
import os
import re
import sys
from collections import Counter
from pathlib import Path

from llm import describe_image, ocr_contract_image


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DATA_DIR = REPO_ROOT / "data"
DEFAULT_IMAGE_DIR = DEFAULT_DATA_DIR / "image"
DEFAULT_PREFIX = "contract"

CHARS_PER_KB_THRESHOLD = 180
MIN_UNIQUE_LINE_RATIO = 0.5
MAX_REPEATED_PREFIX = 15
LINE_PREFIX_RE = re.compile(r"^(?:\s*\d+[\.\)]\s*)?([A-Za-z가-힣][\w\s]{0,20}?)\s*[:：]")
FILLER_PHRASES = (
    "the quick brown fox",
    "lorem ipsum",
    "sample document",
)


def resolve_image_path(raw: Path, image_dir: Path) -> Path:
    """이미지 경로 해석: 그대로 존재하면 그대로, 아니면 image_dir 기준으로 찾는다."""
    if raw.is_absolute() or raw.exists():
        return raw
    return image_dir / raw


def next_index(data_dir: Path, prefix: str) -> int:
    """data 폴더에서 {prefix}_exN.txt 중 사용되지 않은 가장 작은 N(>=1)을 반환.

    max+1이 아니라 빈 번호 채우기 방식이라, 중간 파일이 지워진 뒤에도
    카운터가 계속 부풀지 않고 디렉터리 상태와 일치한다.
    """
    pattern = re.compile(rf"^{re.escape(prefix)}_ex(\d+)\.txt$")
    used: set[int] = set()
    if data_dir.exists():
        for p in data_dir.iterdir():
            m = pattern.match(p.name)
            if m:
                used.add(int(m.group(1)))
    n = 1
    while n in used:
        n += 1
    return n


def analyze_ocr_output(text: str, image_bytes: int) -> tuple[dict, list[str]]:
    """OCR 결과가 환각인지 의심되는 신호를 점검한다.

    반환: (stats, warnings) — warnings가 비어있지 않으면 환각 가능성 있음.
    """
    warnings: list[str] = []
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    chars = len(text)
    image_kb = max(image_bytes / 1024, 1)
    chars_per_kb = chars / image_kb

    # 1) 이미지 크기 대비 출력 길이 — 환각으로 부풀려진 경우 잡힘
    if chars_per_kb > CHARS_PER_KB_THRESHOLD:
        warnings.append(
            f"출력이 이미지 크기 대비 비정상적으로 김 "
            f"({chars:,}자 / {image_kb:.1f}KB = {chars_per_kb:.0f} chars/KB, "
            f"기준 {CHARS_PER_KB_THRESHOLD} 초과)"
        )

    # 2) 줄 반복 — LLM이 같은 패턴을 cycling하며 양을 늘리는 전형적 환각
    if len(lines) >= 10:
        unique_ratio = len(set(lines)) / len(lines)
        if unique_ratio < MIN_UNIQUE_LINE_RATIO:
            warnings.append(
                f"줄 중복률이 너무 높음 (고유 줄 비율 {unique_ratio:.0%}, "
                f"기준 {MIN_UNIQUE_LINE_RATIO:.0%} 미만)"
            )

    # 3) 같은 라벨(예: "Name:", "Score:")이 과도하게 반복 — 양식 cycling 환각
    prefixes = []
    for ln in lines:
        m = LINE_PREFIX_RE.match(ln)
        if m:
            prefixes.append(m.group(1).strip().lower())
    if prefixes:
        top_prefix, top_count = Counter(prefixes).most_common(1)[0]
        if top_count >= MAX_REPEATED_PREFIX:
            warnings.append(
                f"동일 라벨 '{top_prefix}:'이 {top_count}회 반복됨 "
                f"(기준 {MAX_REPEATED_PREFIX} 이상) — 양식이 cycling되는 환각 패턴"
            )

    # 4) 알려진 LLM filler 문구
    lower = text.lower()
    hits = [p for p in FILLER_PHRASES if p in lower]
    if hits:
        warnings.append(f"학습 데이터의 전형적 filler 문구 감지: {hits}")

    stats = {
        "chars": chars,
        "lines": len(lines),
        "unique_lines": len(set(lines)),
        "chars_per_kb": round(chars_per_kb, 1),
    }
    return stats, warnings


async def main() -> int:
    parser = argparse.ArgumentParser(description="사진에서 텍스트를 추출해 data/contract_exN.txt로 저장")
    parser.add_argument("image", type=Path,
                        help=f"OCR할 이미지 (파일명만 주면 {DEFAULT_IMAGE_DIR}에서 찾음)")
    parser.add_argument("--image-dir", type=Path, default=DEFAULT_IMAGE_DIR,
                        help=f"입력 이미지 폴더 (기본: {DEFAULT_IMAGE_DIR})")
    parser.add_argument("--data-dir", type=Path, default=DEFAULT_DATA_DIR,
                        help=f"저장 폴더 (기본: {DEFAULT_DATA_DIR})")
    parser.add_argument("--prefix", default=DEFAULT_PREFIX,
                        help=f"파일명 접두사 (기본: {DEFAULT_PREFIX} → {DEFAULT_PREFIX}_exN.txt)")
    parser.add_argument("--index", type=int, default=None,
                        help="저장할 번호 N을 직접 지정 (생략 시 자동 증가)")
    parser.add_argument("--overwrite", action="store_true",
                        help="--index 지정 시 같은 이름의 파일이 있어도 덮어쓰기")
    parser.add_argument("--verify", action="store_true",
                        help="OCR 전에 이미지 묘사 호출로 vision 모델이 실제로 보고 있는지 확인")
    parser.add_argument("--debug", action="store_true",
                        help="LLM 호출 메타데이터(이미지 바이트, 토큰 수 등)를 stderr로 출력")
    args = parser.parse_args()

    if args.debug:
        os.environ["LLM_DEBUG"] = "1"

    image_path = resolve_image_path(args.image, args.image_dir)
    if not image_path.exists():
        print(f"❌ 이미지 파일 없음: {image_path}", file=sys.stderr)
        if not args.image.is_absolute() and not args.image.exists():
            print(f"   (입력 폴더 확인: {args.image_dir})", file=sys.stderr)
        return 1

    args.data_dir.mkdir(parents=True, exist_ok=True)

    if args.index is not None:
        n = args.index
        out_path = args.data_dir / f"{args.prefix}_ex{n}.txt"
        if out_path.exists() and not args.overwrite:
            print(f"❌ 이미 존재: {out_path} (--overwrite로 덮어쓰기 가능)", file=sys.stderr)
            return 3
    else:
        n = next_index(args.data_dir, args.prefix)
        out_path = args.data_dir / f"{args.prefix}_ex{n}.txt"

    print(f"🖼️  이미지 로드: {image_path}")
    image_bytes = image_path.read_bytes()

    if args.verify:
        print(f"🔍 그라운딩 확인: 이미지 묘사 요청 중...")
        description = await describe_image(image_bytes)
        print(f"   → 모델이 본 이미지: {description}")

    print(f"🔎 OCR 진행 중... (Qwen2-VL 호출)")
    text = await ocr_contract_image(image_bytes)

    preview = text.strip().splitlines()[:5]
    print("📄 출력 미리보기 (처음 5줄):")
    for line in preview:
        print(f"   {line[:120]}")

    stats, warnings = analyze_ocr_output(text, len(image_bytes))
    print(
        f"📊 통계: chars={stats['chars']:,}  lines={stats['lines']}  "
        f"unique_lines={stats['unique_lines']}  "
        f"chars_per_kb={stats['chars_per_kb']}"
    )
    if warnings:
        print("⚠️  환각 가능성 신호:", file=sys.stderr)
        for w in warnings:
            print(f"   - {w}", file=sys.stderr)
        print(
            "   → 모델이 이미지를 실제로 읽지 않고 학습 패턴을 생성했을 수 있습니다.",
            file=sys.stderr,
        )
        print(
            "   → --verify 옵션으로 모델이 어떤 이미지를 보고 있는지 확인하거나, "
            "QWEN_OCR_MODEL 환경변수로 더 큰 Qwen-VL 모델로 바꿔보세요.",
            file=sys.stderr,
        )

    out_path.write_text(text, encoding="utf-8")
    print(f"💾 저장 완료: {out_path}  ({len(text):,}자)")
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
