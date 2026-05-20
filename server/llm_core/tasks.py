"""고수준 LLM 태스크.

- 계약서 이미지 OCR
- 계약서 텍스트의 독소조항 분석

내부적으로 `OllamaClient`와 `LLMConfig` 프리셋을 사용한다.
독소조항 분석은 커스텀 모델(`TOXIC_DETECTOR_MODEL`)이 등록돼 있으면 그쪽을 쓰고,
없으면 일반 텍스트 모델에 전체 프롬프트를 직접 주입하는 방식으로 폴백한다.
"""
from __future__ import annotations

import re
from typing import Any, Optional

from .client import OllamaClient, OllamaError, get_client
from .config import (
    get_negotiation_config,
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


NEGOTIATION_SCRIPT_PROMPT = """당신은 세입자가 임대인에게 보낼 협상 메시지를 작성합니다.

세입자 입장에서 아래 독소조항을 임대인과 협상하기 위한 정중한 한국어 스크립트를 작성하세요.
문자메시지·카카오톡·이메일 본문으로 그대로 보낼 수 있는 완성된 글이어야 합니다.

[작성 규칙]
1. 한국어 존댓말. 3~5문장 분량. 너무 길지 않게.
2. 다음 흐름을 따른다:
   (a) "임대인님 안녕하세요" 같은 정중한 인사
   (b) 협상하고 싶은 조항을 짧게 인용·언급
   (c) 어떤 점이 부담스러운지 + 관련 법령(주택임대차보호법 / 민법 등) 한 가지 이상 근거 제시
   (d) 어떻게 수정 또는 삭제하면 좋을지 구체적인 대안 제안
   (e) "검토 부탁드립니다" 식 정중한 마무리
3. 절대 금지: 협박·비난·반말·욕설, "위약금 청구하겠다" 같은 압박, 임대인을 가르치는 어조.
4. 머리말("협상 스크립트:" 등 라벨)이나 부가 설명 없이 메시지 본문만 출력.
5. 결과는 평문 텍스트. JSON·마크다운·코드블록 사용 금지."""


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


_SCRIPT_HEADER_RE = re.compile(
    r"^\s*(협상\s*스크립트|메시지|스크립트|message|negotiation\s*script)\s*[:：]\s*\n",
    re.IGNORECASE,
)


def _clean_negotiation_script(text: str) -> str:
    """모델이 종종 첫 줄에 붙이는 '협상 스크립트:' 류 머리말을 제거한다."""
    cleaned = text.strip()
    cleaned = _SCRIPT_HEADER_RE.sub("", cleaned, count=1)
    return cleaned.strip()


async def generate_negotiation_scripts(
    toxic_clauses: list[dict[str, str]],
    *,
    client: Optional[OllamaClient] = None,
) -> list[str]:
    """각 독소조항에 대해 임대인과의 협상에 그대로 쓸 수 있는 메시지를 생성한다.

    Args:
        toxic_clauses: `analyze_contract`가 반환한 toxic_clauses 형식 리스트.
            각 항목은 `clause` / `reason` / `recommendation`을 갖는다.
        client: 재사용할 Ollama 클라이언트.

    Returns:
        toxic_clauses와 같은 길이·순서의 스크립트 문자열 리스트.
        개별 호출이 실패한 항목은 빈 문자열로 채워 자리만 맞춘다(인덱스 정렬 유지).
    """
    if not toxic_clauses:
        return []
    cli = client or get_client()
    cfg = get_negotiation_config()

    scripts: list[str] = []
    for clause in toxic_clauses:
        prompt = (
            NEGOTIATION_SCRIPT_PROMPT
            + f"\n\n[독소 조항 원문]\n{clause.get('clause', '')}"
            + f"\n\n[조항의 문제점]\n{clause.get('reason', '')}"
            + f"\n\n[수정 방향]\n{clause.get('recommendation', '')}"
            + "\n\n이제 위 조항을 협상하기 위한 메시지 본문을 작성하세요."
        )
        try:
            text = await cli.generate(prompt, cfg)
        except OllamaError:
            text = ""
        scripts.append(_clean_negotiation_script(text))
    return scripts


_ADDRESS_UNKNOWN = "주소 불명"

ADDRESS_EXTRACT_PROMPT = (
    "계약서 텍스트에서 임차주택(계약 대상 건물)의 주소를 추출하세요.\n"
    "반드시 아래 JSON 형식만 출력하세요: {\"address\": \"전체 주소\"}\n"
    f"주소를 찾을 수 없으면 {{\"address\": \"{_ADDRESS_UNKNOWN}\"}}을 출력하세요.\n\n"
    "[계약서 내용]\n{contract_text}"
)


async def extract_contract_address(
    contract_text: str,
    *,
    client: Optional[OllamaClient] = None,
) -> str:
    """계약서 텍스트에서 임차주택 주소를 추출한다."""
    cli = client or get_client()
    cfg = get_text_config().merged(temperature=0.0, format="json")
    prompt = ADDRESS_EXTRACT_PROMPT.format(contract_text=contract_text[:3000])
    try:
        raw = await cli.generate_json(prompt, cfg)
        return str(raw.get("address") or _ADDRESS_UNKNOWN).strip() or _ADDRESS_UNKNOWN
    except OllamaError:
        return _ADDRESS_UNKNOWN


async def analyze_contract(
    contract_text: str,
    *,
    client: Optional[OllamaClient] = None,
    include_scripts: bool = True,
) -> dict[str, Any]:
    """계약서 텍스트에서 독소조항을 분석해 JSON으로 반환한다.

    `include_scripts=True`(기본)면 발견된 독소조항마다 임대인 협상용 메시지를
    `negotiation_script` 필드에 추가로 채워준다. 호출 횟수가 독소조항 개수만큼
    늘어나니, 단순 검출만 필요한 경로(배치 평가 등)에서는 False로 호출한다.
    """
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
    result = _normalize_analysis(raw)

    if include_scripts and result["toxic_clauses"]:
        scripts = await generate_negotiation_scripts(result["toxic_clauses"], client=cli)
        for clause, script in zip(result["toxic_clauses"], scripts):
            clause["negotiation_script"] = script

    return result
