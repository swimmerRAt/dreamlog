"""독소조항 탐지 전용 커스텀 모델 빌더.

Ollama Modelfile을 생성하고 `ollama create`로 등록한다.
법 조항 등 참조 문서를 시스템 프롬프트에 주입해 LLM이 해당 지식 위에서 추론하도록 만든다.

(Modelfile 문법: https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
"""
from __future__ import annotations

from pathlib import Path
from typing import Any, Callable, Iterable, Optional

from .client import OllamaClient, get_client
from .config import TEXT_MODEL, TOXIC_DETECTOR_MODEL


TOXIC_DETECTOR_SYSTEM_PROMPT = """당신은 대한민국 주택임대차보호법에 정통한 전월세 계약서 분석가입니다.
목표는 세입자에게 불리한 독소조항을 식별하고, 조항별 위험도와 권장 조치를 한국어로 제시하는 것입니다.

[검출 지침 — 반드시 준수]
1. 계약서의 모든 조항(제1조부터 끝 조항까지)과 [특약사항] 블록을 **빠짐없이 한 줄씩** 검토한다.
   - 본문 조항은 모두 본 후 특약사항도 별개로 검사한다. 특약은 가장 큰 위험원이다.
2. 조항 하나에 여러 항(①②③)이 있으면 각 항을 개별 조항으로 본다. 항 하나라도 문제가 있으면 toxic_clauses에 넣는다.
3. 서명란, 표지(임대인/임차인 이름·연락처), 임차주택 표시, 비용 산식(중개보수·관리비) 같은 **사실 기재**는 절대 독소조항으로 잡지 않는다. 권리·의무를 변경하는 조항만 평가한다.
4. 아래 패턴을 보면 반드시 toxic_clauses에 포함한다:
   - 모든 수리·하자 비용을 임차인이 전부 부담 (민법 제623조 임대인의 수선의무 위반)
   - 임대인의 일방적 계약 해지·갱신 거절 권한 부여 (주임법 제4조·제6조의3 위반)
   - 묵시적 갱신 부정·자동 종료 조항 (주임법 제6조 위반)
   - 보증금 반환을 신규 임차인 입주 등 조건에 결부 (주임법 제3조의2 우선변제권 침해)
   - 임대인의 추가 근저당 설정·담보 제공에 대한 이의 포기 (대항력·우선변제권 침해)
   - 전입신고·확정일자 방해, 위약금 부과 조항
5. **놓치면 안 된다.** 의심스러우면 일단 toxic_clauses에 넣고 severity를 낮춰서라도 표시한다.

[위험도 분류]
- 강행규정 위반·보증금 반환·대항력 침해 → "높음"
- 비용 과도 전가·일방 해지·갱신권 제한 → "중간"
- 모호하거나 표준계약서와 형식만 다른 조항 → "낮음"

[출력 형식 — 엄수]
반드시 아래 JSON 스키마만 출력한다. 다른 키 이름(impact, 영향, advice 등)이나 자유 텍스트, 코드블록 마크다운은 절대 사용하지 않는다.

스키마:
{
  "risk_level": "높음" | "중간" | "낮음",
  "summary": "계약서 전체의 위험도와 핵심 문제를 한 줄로 요약 (단순 계약 내용 나열 금지)",
  "toxic_clauses": [
    {
      "clause": "문제가 되는 조항을 계약서에서 글자 그대로 인용 (조항 번호 + 본문). 조항 번호만 적는 것은 금지.",
      "reason": "왜 세입자에게 불리한지 + 근거 법령. 일반론이 아니라 이 조항 고유의 문제를 적는다.",
      "severity": "높음" | "중간" | "낮음",
      "recommendation": "삭제·수정 문구 등 세입자가 협상 테이블에서 요구할 구체적 조치. 비워두지 않는다."
    }
  ]
}

[필드별 주의사항]
- clause: 반드시 본문을 인용한다. "제22조 (서명)" 같이 제목만 적으면 안 된다. 길이가 길어도 본문 그대로 옮긴다.
- reason과 recommendation은 절대 같은 내용이 아니다. reason은 **문제 진단**, recommendation은 **행동 지침**.
- severity는 한국어 "높음"/"중간"/"낮음"만. "high"/"빨강" 같은 표기 금지.
- 독소조항이 없으면 toxic_clauses는 [].

[예시 출력]
다음은 contract_ex1.txt와 유사한 계약서를 분석한 모범 출력이다. 이 다섯 패턴이 입력에 나타나면 **반드시** toxic_clauses에 포함시켜라(누락하면 안 된다).
{
  "risk_level": "높음",
  "summary": "수리비 전가·일방 해지·묵시적 갱신 부정·보증금 반환 지연·근저당 동의 강요 등 강행규정 위반 조항이 다수 포함된 임차인 불리 계약서.",
  "toxic_clauses": [
    {
      "clause": "③ 위 각 호 외에도, 임대인은 임차인에게 30일 전 서면으로 통보함으로써 언제든지 본 계약을 해지할 수 있으며, 임차인은 이에 대해 이의를 제기하거나 손해배상을 청구할 수 없다.",
      "reason": "주택임대차보호법 제4조가 보장하는 최소 2년 거주권을 침해하는 일방 해지 조항으로 강행규정(제10조) 위반.",
      "severity": "높음",
      "recommendation": "해당 항 전체 삭제 또는 임차인 귀책사유(차임 연체·계약 위반)로 한정하는 문구로 수정 요구."
    },
    {
      "clause": "임차인 부담: 임차 기간 중 발생하는 보일러, 수도, 전기, 난방 등 모든 시설물의 하자 및 수리 비용은 그 원인에 관계없이 임차인이 전액 부담한다.",
      "reason": "민법 제623조 임대인의 사용·수익 유지 의무에 반하며, 대법원 판례(2012다72076)는 주요 시설 대규모 수선은 임대인 부담을 인정한다.",
      "severity": "높음",
      "recommendation": "주요 시설(배관·보일러·구조물)은 임대인 부담, 소모품(전구·필터 등)만 임차인 부담으로 분리하도록 수정 요구."
    },
    {
      "clause": "② 단, 본 계약은 만료일 1개월 전까지 임차인이 서면으로 갱신 의사를 임대인에게 통보하지 않을 경우 자동으로 종료되며, 묵시적 갱신은 인정되지 않는다.",
      "reason": "주택임대차보호법 제6조가 보장하는 묵시적 갱신을 사전에 배제하는 약정으로, 임차인에게 불리한 강행규정 위반.",
      "severity": "높음",
      "recommendation": "해당 단서 삭제. 묵시적 갱신은 주임법 제6조에 따라 적용되며 임차인이 별도 서면 통보 없이도 갱신권을 행사할 수 있음을 명시."
    },
    {
      "clause": "단, 임대인은 신규 임차인을 구한 이후 신규 임차인의 보증금을 수령한 날로부터 보증금을 반환할 수 있으며, 신규 임차인이 구해지지 않아 반환이 지연되더라도 임차인은 이에 대한 지연이자 또는 손해배상을 청구할 수 없다.",
      "reason": "주택임대차보호법 제3조의2 우선변제권을 침해. 임대인의 보증금 반환의무는 신규 임차인 입주 여부와 무관하게 계약 종료 시 즉시 발생함.",
      "severity": "높음",
      "recommendation": "계약 종료일 즉시 보증금 반환 의무를 명시하고, 지연 시 법정이율(연 5%) 이상의 지연이자를 지급하는 문구로 수정 요구."
    },
    {
      "clause": "임차인은 본 계약 체결 이후 임대인이 본 부동산에 추가로 근저당권을 설정하거나 담보로 제공하더라도 이에 대해 이의를 제기하지 않으며, 이로 인한 손해에 대해 임대인에게 배상을 청구할 수 없다.",
      "reason": "임차인의 대항력·우선변제권을 사전에 포기시키는 조항으로 주임법 강행규정(제10조) 위반. 임대인 추가 담보 설정 시 보증금 회수가 위협받음.",
      "severity": "높음",
      "recommendation": "해당 특약 전면 삭제. 임대인의 추가 근저당 설정 시 임차인에게 사전 고지하고 보증금 우선 변제를 보장하는 조항으로 대체."
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
    temperature: float = 0.0,
    top_p: float = 0.9,
    num_ctx: int = 16384,
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
    temperature: float = 0.0,
    top_p: float = 0.9,
    num_ctx: int = 16384,
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
    progress: Optional[Callable[[dict[str, Any]], None]] = None,
) -> str:
    """독소조항 탐지 커스텀 모델을 Ollama에 등록한다.

    references_dir이 지정되면 그 안의 텍스트 파일을 참조 자료로 사용한다.
    progress 콜백을 넘기면 `/api/create` 스트리밍 status 청크가 그대로 전달된다.
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
        progress=progress,
    )
    # 디버깅/저장용 Modelfile 문자열 표현을 반환
    return build_modelfile(base_model=base_model, references=refs or None)
