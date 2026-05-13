"""독소조항 탐지 전용 커스텀 모델 빌더.

Ollama Modelfile을 생성하고 `ollama create`로 등록한다.
법 조항 등 참조 문서를 시스템 프롬프트에 주입해 LLM이 해당 지식 위에서 추론하도록 만든다.

(Modelfile 문법: https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
"""
from __future__ import annotations

from pathlib import Path
from typing import Any, Iterable, Optional

from .client import OllamaClient, get_client
from .config import TEXT_MODEL, TOXIC_DETECTOR_MODEL


TOXIC_DETECTOR_SYSTEM_PROMPT = """당신은 대한민국 주택임대차보호법에 정통한 전월세 계약서 분석가입니다.
목표는 세입자에게 불리한 독소조항을 식별하고, 조항별 위험도와 권장 조치를 한국어로 제시하는 것입니다.

[판단 원칙]
- 주택임대차보호법, 민법 임대차 관련 규정, 표준임대차계약서를 기준으로 삼는다.
- 세입자의 대항력·우선변제권·계약갱신요구권·전세금 반환을 침해하는 조항은 위험도 "높음".
- 비용 부담을 과도하게 전가하거나 일방적인 해지·위약 조건은 위험도 "중간".
- 표준계약서와 차이가 있으나 즉시 불이익으로 이어지지 않는 모호한 조항은 위험도 "낮음".
- 법령에 위반되거나 무효일 가능성이 있는 조항은 이유에 그 근거를 명시한다.

[출력 형식 — 엄수]
반드시 아래 JSON 스키마만 출력한다. 다른 키 이름(impact, 영향, advice 등)이나 자유 텍스트는 절대 사용하지 않는다.

스키마:
{
  "risk_level": "높음" | "중간" | "낮음",
  "summary": "계약서 전체에 대한 한 줄 요약 (한국어)",
  "toxic_clauses": [
    {
      "clause": "문제가 되는 조항 원문 (계약서에서 그대로 인용)",
      "reason": "왜 세입자에게 불리한지, 어떤 법령·원칙에 어긋나는지",
      "severity": "높음" | "중간" | "낮음",
      "recommendation": "세입자가 취해야 할 구체적 조치"
    }
  ]
}

각 toxic_clauses 항목은 반드시 네 개 필드(clause, reason, severity, recommendation)를 모두 포함해야 한다. severity는 반드시 "높음"·"중간"·"낮음" 중 하나의 한국어 단어여야 한다. 독소조항이 없으면 toxic_clauses는 빈 배열([])로 둔다.

[예시 출력]
{
  "risk_level": "높음",
  "summary": "임대인에게 일방 해지·갱신 거절 권한이 과도하게 부여된 계약서.",
  "toxic_clauses": [
    {
      "clause": "임대인은 임차인에게 30일 전 서면으로 통보함으로써 언제든지 본 계약을 해지할 수 있으며, 임차인은 이의를 제기하거나 손해배상을 청구할 수 없다.",
      "reason": "주택임대차보호법 제4조가 보장하는 최소 2년 거주권을 침해하는 일방 해지 조항으로 강행규정(제10조) 위반 가능성이 있다.",
      "severity": "높음",
      "recommendation": "해당 조항 삭제 또는 임차인 귀책사유로 한정하는 문구로 수정 요구."
    }
  ]
}
"""


def _compose_system_prompt(
    system_prompt: str,
    references: Optional[Iterable[str]] = None,
) -> str:
    full = system_prompt.strip()
    if references:
        ref_block = "\n\n".join(r.strip() for r in references if r and r.strip())
        if ref_block:
            full += "\n\n[참조 자료]\n" + ref_block
    return full


def build_model_spec(
    *,
    base_model: str = TEXT_MODEL,
    system_prompt: str = TOXIC_DETECTOR_SYSTEM_PROMPT,
    references: Optional[Iterable[str]] = None,
    temperature: float = 0.1,
    top_p: float = 0.9,
    num_ctx: int = 8192,
    repeat_penalty: float = 1.1,
) -> dict[str, Any]:
    """Ollama 신 `/api/create` API에 보낼 구조화 페이로드를 만든다.

    references에 들어온 텍스트는 시스템 프롬프트 하단에 참조 자료로 덧붙는다.
    """
    return {
        "from": base_model,
        "system": _compose_system_prompt(system_prompt, references),
        "parameters": {
            "temperature": temperature,
            "top_p": top_p,
            "num_ctx": num_ctx,
            "repeat_penalty": repeat_penalty,
            "stop": ["<|eot_id|>"],
        },
    }


def build_modelfile(
    *,
    base_model: str = TEXT_MODEL,
    system_prompt: str = TOXIC_DETECTOR_SYSTEM_PROMPT,
    references: Optional[Iterable[str]] = None,
    temperature: float = 0.1,
    top_p: float = 0.9,
    num_ctx: int = 8192,
    repeat_penalty: float = 1.1,
) -> str:
    """디버깅·저장용 Modelfile 문자열 표현.

    실제 모델 등록은 `build_model_spec()` + `client.create_model()`을 사용한다.
    """
    parts: list[str] = []
    parts.append(f"FROM {base_model}")
    parts.append("")
    parts.append(f"PARAMETER temperature {temperature}")
    parts.append(f"PARAMETER top_p {top_p}")
    parts.append(f"PARAMETER num_ctx {num_ctx}")
    parts.append(f"PARAMETER repeat_penalty {repeat_penalty}")
    parts.append('PARAMETER stop "<|eot_id|>"')
    parts.append("")

    full_system = _compose_system_prompt(system_prompt, references)
    escaped = full_system.replace("\\", "\\\\").replace('"', '\\"')
    parts.append(f'SYSTEM """{escaped}"""')
    parts.append("")
    return "\n".join(parts)


def load_references_from_dir(directory: str | Path) -> list[str]:
    """디렉토리 내 모든 .txt/.md 파일을 참조 자료로 읽어들인다.

    학습 데이터(법 조항 모음 등)를 파일로 관리할 때 사용.
    """
    path = Path(directory)
    if not path.exists():
        return []
    texts: list[str] = []
    for f in sorted(path.iterdir()):
        if f.suffix.lower() in {".txt", ".md"} and f.is_file():
            texts.append(f.read_text(encoding="utf-8"))
    return texts


async def create_toxic_detector(
    *,
    name: str = TOXIC_DETECTOR_MODEL,
    base_model: str = TEXT_MODEL,
    references_dir: Optional[str | Path] = None,
    extra_references: Optional[Iterable[str]] = None,
    client: Optional[OllamaClient] = None,
) -> str:
    """독소조항 탐지 커스텀 모델을 Ollama에 등록한다.

    references_dir이 지정되면 그 안의 텍스트 파일을 참조 자료로 사용한다.
    반환값은 등록에 사용한 Modelfile 문자열 (디버깅/저장용).
    """
    cli = client or get_client()
    await cli.ensure_model(base_model)

    refs: list[str] = []
    if references_dir is not None:
        refs.extend(load_references_from_dir(references_dir))
    if extra_references:
        refs.extend(extra_references)

    spec = build_model_spec(base_model=base_model, references=refs or None)
    await cli.create_model(
        name,
        from_model=spec["from"],
        system=spec["system"],
        parameters=spec["parameters"],
    )
    # 디버깅/저장용 Modelfile 문자열 표현을 반환
    return build_modelfile(base_model=base_model, references=refs or None)
