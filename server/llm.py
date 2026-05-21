"""LLM 분석 모듈 — 계약서 텍스트에서 독소조항과 주소를 추출한다."""
from __future__ import annotations

import asyncio
from typing import Any

from llm_core import analyze_contract, extract_contract_address, get_client

__all__ = ["analyze", "get_client"]


async def analyze(
    contract_text: str,
    *,
    include_scripts: bool = True,
) -> dict[str, Any]:
    """계약서 텍스트를 분석해 독소조항·주소·위험도를 반환한다.

    반환 스키마:
    {
        "address":      "임차주택 주소",
        "risk_level":   "높음" | "중간" | "낮음",
        "summary":      "계약서 전체 위험도 요약",
        "toxic_clauses": [
            {
                "clause":             "조항 원문",
                "reason":             "문제점",
                "severity":           "높음" | "중간" | "낮음",
                "recommendation":     "권장 조치",
                "negotiation_script": "협상 메시지 (include_scripts=True 시)"
            }
        ]
    }
    """
    client = get_client()
    result, address = await asyncio.gather(
        analyze_contract(contract_text, client=client, include_scripts=include_scripts),
        extract_contract_address(contract_text, client=client),
    )
    result["address"] = address
    return result
