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
)
from .modelfile import TOXIC_DETECTOR_SYSTEM_PROMPT
from .qwen_ocr import qwen_vision_generate


_VALID_RISK_LEVELS = {"높음", "중간", "낮음"}


def _coerce_severity(value: Any) -> str:
    """severity 값이 빠지거나 다른 라벨이면 '중간'으로 정리한다."""
    if not value:
        return "중간"
    s = str(value).strip()
    if s in _VALID_RISK_LEVELS:
        return s
    # 신호등 라벨(학습 데이터에서 자주 등장) → 출력 라벨로 매핑
    mapping = {"빨강": "높음", "노랑": "중간", "초록": "낮음",
               "high": "높음", "medium": "중간", "low": "낮음"}
    return mapping.get(s.lower(), "중간")


def _normalize_clause(raw: Any) -> dict[str, str]:
    """LLM이 다른 키 이름을 쓰더라도 응답 스키마에 맞게 정규화."""
    if not isinstance(raw, dict):
        return {"clause": str(raw), "reason": "", "severity": "중간", "recommendation": ""}
    clause = raw.get("clause") or raw.get("조항") or ""
    reason = (raw.get("reason") or raw.get("impact") or raw.get("이유")
              or raw.get("근거") or "")
    severity = _coerce_severity(raw.get("severity") or raw.get("위험도") or raw.get("risk"))
    recommendation = (raw.get("recommendation") or raw.get("advice")
                      or raw.get("권장") or raw.get("조치") or "")
    return {
        "clause": str(clause),
        "reason": str(reason),
        "severity": severity,
        "recommendation": str(recommendation),
    }


def _normalize_analysis(raw: dict[str, Any]) -> dict[str, Any]:
    """analyze_contract 응답을 라우터 응답 스키마에 맞게 정리."""
    risk = raw.get("risk_level") or raw.get("위험도") or "알 수 없음"
    if isinstance(risk, str) and risk in {"빨강", "노랑", "초록"}:
        risk = {"빨강": "높음", "노랑": "중간", "초록": "낮음"}[risk]
    clauses_raw = raw.get("toxic_clauses") or raw.get("독소조항") or []
    if not isinstance(clauses_raw, list):
        clauses_raw = []
    return {
        "risk_level": str(risk),
        "summary": str(raw.get("summary") or raw.get("요약") or ""),
        "toxic_clauses": [_normalize_clause(c) for c in clauses_raw],
        **({"error": raw["error"]} if "error" in raw else {}),
    }


CONTRACT_OCR_PROMPT = (
    "이미지에 적힌 모든 글자를 그대로 옮겨 적어주세요. "
    "줄 순서와 줄바꿈은 보이는 대로 유지하고, "
    "설명·요약·해석은 덧붙이지 마세요. "
    "텍스트가 전혀 없으면 [NO_TEXT]만 출력하고, "
    "읽기 어려운 글자는 [?]로 표시하세요."
)


IMAGE_DESCRIPTION_PROMPT = """이미지를 한 줄로 사실 그대로 묘사하세요.
관찰 가능한 시각 정보만 적으세요: 어떤 종류의 문서/사진인지, 주요 색상,
글자가 있다면 인쇄/손글씨/표 형식인지, 대략 몇 줄인지.
추론·해석·내용 요약은 절대 하지 마세요. 한 줄로만 응답하세요."""


FALLBACK_ANALYSIS_PROMPT = (
    TOXIC_DETECTOR_SYSTEM_PROMPT
    + "\n\n[계약서 내용]\n{contract_text}\n"
)


async def describe_image(
    image_bytes: bytes,
    *,
    client: Optional[OllamaClient] = None,  # noqa: ARG001 — 호환을 위해 시그니처 유지
) -> str:
    """이미지에 무엇이 보이는지 한 줄로 묘사한다(그라운딩 검증용)."""
    text = await qwen_vision_generate(
        IMAGE_DESCRIPTION_PROMPT,
        image_bytes,
        max_new_tokens=200,
    )
    return text.strip()


async def ocr_contract_image(
    image_bytes: bytes,
    *,
    client: Optional[OllamaClient] = None,  # noqa: ARG001 — 호환을 위해 시그니처 유지
) -> str:
    """계약서 이미지에서 텍스트를 추출한다."""
    return await qwen_vision_generate(
        CONTRACT_OCR_PROMPT,
        image_bytes,
        max_new_tokens=2048,
    )


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
        cfg = get_text_config().merged(temperature=0.1, format="json", num_ctx=16384)
        prompt = FALLBACK_ANALYSIS_PROMPT.format(contract_text=contract_text)

    try:
        raw = await cli.generate_json(prompt, cfg)
    except OllamaError as e:
        return {
            "risk_level": "알 수 없음",
            "summary": "분석 결과 파싱 실패",
            "toxic_clauses": [],
            "error": str(e),
        }
    return _normalize_analysis(raw)
