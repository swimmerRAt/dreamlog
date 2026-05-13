"""고수준 LLM 태스크.

- 계약서 이미지 OCR
- 계약서 텍스트의 독소조항 분석

내부적으로 `OllamaClient`와 `LLMConfig` 프리셋을 사용한다.
독소조항 분석은 커스텀 모델(`TOXIC_DETECTOR_MODEL`)이 등록돼 있으면 그쪽을 쓰고,
없으면 일반 텍스트 모델에 전체 프롬프트를 직접 주입하는 방식으로 폴백한다.
"""
from __future__ import annotations

from typing import Any, Optional

from .client import OllamaClient, OllamaError, get_client
from .config import (
    get_text_config,
    get_toxic_detector_config,
    get_vision_config,
)
from .modelfile import TOXIC_DETECTOR_SYSTEM_PROMPT


CONTRACT_OCR_PROMPT = """이 이미지는 한국의 전월세(임대차) 계약서입니다.
이미지에 있는 모든 텍스트를 그대로 추출해주세요.
계약서에 있는 내용을 빠짐없이 정확하게 텍스트로 변환해주세요.
다른 설명 없이 추출된 텍스트만 출력해주세요."""


FALLBACK_ANALYSIS_PROMPT = (
    TOXIC_DETECTOR_SYSTEM_PROMPT
    + "\n\n[계약서 내용]\n{contract_text}\n"
)


async def ocr_contract_image(
    image_bytes: bytes,
    *,
    client: Optional[OllamaClient] = None,
) -> str:
    """계약서 이미지에서 텍스트를 추출한다."""
    cli = client or get_client()
    cfg = get_vision_config()
    return await cli.generate(CONTRACT_OCR_PROMPT, cfg, images=[image_bytes])


async def analyze_contract(
    contract_text: str,
    *,
    client: Optional[OllamaClient] = None,
) -> dict[str, Any]:
    """계약서 텍스트에서 독소조항을 분석해 JSON으로 반환한다."""
    cli = client or get_client()
    cfg = get_toxic_detector_config()

    if await cli.has_model(cfg.model):
        # 커스텀 모델은 시스템 프롬프트에 분석 지침이 이미 포함됨
        prompt = contract_text
    else:
        # 폴백: 일반 텍스트 모델에 전체 프롬프트를 직접 주입
        cfg = get_text_config().merged(temperature=0.1, format="json", num_ctx=8192)
        prompt = FALLBACK_ANALYSIS_PROMPT.format(contract_text=contract_text)

    try:
        return await cli.generate_json(prompt, cfg)
    except OllamaError as e:
        return {
            "risk_level": "알 수 없음",
            "summary": "분석 결과 파싱 실패",
            "toxic_clauses": [],
            "error": str(e),
        }
