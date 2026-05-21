"""OCR 모듈 — 계약서 이미지에서 텍스트를 추출한다."""
from __future__ import annotations

from llm_core import describe_image, ocr_contract_image

__all__ = ["extract_text", "verify_image"]


async def extract_text(image_bytes: bytes) -> str:
    """계약서 이미지를 OCR해서 텍스트로 반환한다."""
    return await ocr_contract_image(image_bytes)


async def verify_image(image_bytes: bytes) -> str:
    """이미지가 무엇인지 한 줄로 묘사한다 (그라운딩 검증용)."""
    return await describe_image(image_bytes)
