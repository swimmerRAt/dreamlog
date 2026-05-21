"""LLM 분석 모듈 — 계약서 텍스트에서 독소조항과 주소를 추출한다."""
from __future__ import annotations

import asyncio
from typing import Any
import traceback

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
    
    # return_exceptions=True를 사용해서 예외를 반환값으로 받음
    results = await asyncio.gather(
        analyze_contract(contract_text, client=client, include_scripts=include_scripts),
        extract_contract_address(contract_text, client=client),
        return_exceptions=True
    )
    
    result = results[0]
    address = results[1]
    
    # 예외를 확인하고 명확한 에러 메시지 생성
    if isinstance(result, Exception):
        tb_str = traceback.format_exception(type(result), result, result.__traceback__)
        print(f"[ERROR] analyze_contract failed:\n{''.join(tb_str)}", flush=True)
        raise result
    
    if isinstance(address, Exception):
        tb_str = traceback.format_exception(type(address), address, address.__traceback__)
        print(f"[ERROR] extract_contract_address failed:\n{''.join(tb_str)}", flush=True)
        raise address
    
    result["address"] = address
    return result
