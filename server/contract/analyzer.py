import base64
import httpx
import os
import json
from dotenv import load_dotenv

load_dotenv()

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
VISION_MODEL = os.getenv("VISION_MODEL", "llava")
TEXT_MODEL = os.getenv("TEXT_MODEL", "llama3.2")

OCR_PROMPT = """이 이미지는 한국의 전월세(임대차) 계약서입니다.
이미지에 있는 모든 텍스트를 그대로 추출해주세요.
계약서에 있는 내용을 빠짐없이 정확하게 텍스트로 변환해주세요.
다른 설명 없이 추출된 텍스트만 출력해주세요."""

ANALYSIS_PROMPT = """당신은 전월세(임대차) 계약서 전문 법률 분석가입니다.
아래 계약서 내용을 분석하여 세입자에게 불리한 독소조항을 찾아주세요.

[계약서 내용]
{contract_text}

다음 JSON 형식으로 답변해주세요:
{{
  "risk_level": "높음|중간|낮음",
  "summary": "전체 계약서에 대한 한 줄 요약",
  "toxic_clauses": [
    {{
      "clause": "문제가 되는 조항 원문",
      "reason": "왜 불리한지 설명",
      "severity": "높음|중간|낮음",
      "recommendation": "세입자가 취해야 할 조치"
    }}
  ]
}}

독소조항이 없으면 toxic_clauses를 빈 배열로 반환하세요.
반드시 유효한 JSON만 반환하고 다른 텍스트는 포함하지 마세요."""


async def ocr_image(image_bytes: bytes, content_type: str) -> str:
    image_b64 = base64.b64encode(image_bytes).decode("utf-8")

    payload = {
        "model": VISION_MODEL,
        "prompt": OCR_PROMPT,
        "images": [image_b64],
        "stream": False,
    }

    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(f"{OLLAMA_BASE_URL}/api/generate", json=payload)
        response.raise_for_status()
        return response.json()["response"]


async def analyze_contract(contract_text: str) -> dict:
    prompt = ANALYSIS_PROMPT.format(contract_text=contract_text)

    payload = {
        "model": TEXT_MODEL,
        "prompt": prompt,
        "stream": False,
        "format": "json",
    }

    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(f"{OLLAMA_BASE_URL}/api/generate", json=payload)
        response.raise_for_status()
        raw = response.json()["response"]

    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        # JSON 파싱 실패 시 원본 텍스트를 감싸서 반환
        return {
            "risk_level": "알 수 없음",
            "summary": "분석 결과 파싱 실패",
            "toxic_clauses": [],
            "raw_response": raw,
        }


async def check_ollama_health() -> bool:
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(f"{OLLAMA_BASE_URL}/api/tags")
            return response.status_code == 200
    except Exception:
        return False
