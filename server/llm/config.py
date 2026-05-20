"""LLM 설정값 정의.

Ollama 모델 호출에 사용할 파라미터를 한 곳에서 관리한다.
환경 변수로 모델명·서버 주소 등을 오버라이드할 수 있다.
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field, asdict
from typing import Any, Optional

from dotenv import load_dotenv

load_dotenv()


OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_TIMEOUT: float = float(os.getenv("OLLAMA_TIMEOUT", "120"))

VISION_MODEL: str = os.getenv("VISION_MODEL", "llava")
TEXT_MODEL: str = os.getenv("TEXT_MODEL", "llama3.2")
TOXIC_DETECTOR_MODEL: str = os.getenv("TOXIC_DETECTOR_MODEL", "dreamlog-toxic-detector")


@dataclass
class LLMConfig:
    """Ollama generate/chat 호출에 전달할 설정 묶음.

    `model`은 Ollama에 등록된 모델 태그.
    `options`에 들어가는 값들은 Ollama API의 `options` 필드로 직렬화된다.
    `format`이 "json"이면 모델 응답을 JSON으로 강제한다.
    """

    model: str
    temperature: float = 0.2
    top_p: float = 0.9
    top_k: int = 40
    num_ctx: int = 4096
    num_predict: int = -1
    repeat_penalty: float = 1.1
    seed: Optional[int] = None
    stop: list[str] = field(default_factory=list)
    system: Optional[str] = None
    format: Optional[str] = None  # "json" 또는 None

    def to_options(self) -> dict[str, Any]:
        opts: dict[str, Any] = {
            "temperature": self.temperature,
            "top_p": self.top_p,
            "top_k": self.top_k,
            "num_ctx": self.num_ctx,
            "num_predict": self.num_predict,
            "repeat_penalty": self.repeat_penalty,
        }
        if self.seed is not None:
            opts["seed"] = self.seed
        if self.stop:
            opts["stop"] = self.stop
        return opts

    def to_payload(self, prompt: str, *, images: Optional[list[str]] = None) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "options": self.to_options(),
        }
        if self.system:
            payload["system"] = self.system
        if self.format:
            payload["format"] = self.format
        if images:
            payload["images"] = images
        return payload

    def merged(self, **overrides: Any) -> "LLMConfig":
        """일부 필드만 덮어쓴 새 설정을 반환."""
        data = asdict(self)
        data.update(overrides)
        return LLMConfig(**data)


def get_vision_config() -> LLMConfig:
    """이미지 OCR용 비전 모델 설정. 낮은 temperature로 정확한 텍스트 추출."""
    return LLMConfig(
        model=VISION_MODEL,
        temperature=0.0,
        top_p=1.0,
        num_ctx=4096,
    )


def get_text_config() -> LLMConfig:
    """일반 텍스트 생성용 설정."""
    return LLMConfig(
        model=TEXT_MODEL,
        temperature=0.3,
        top_p=0.9,
    )


def get_negotiation_config() -> LLMConfig:
    """협상 스크립트 생성용 텍스트 모델 설정.

    토픽이 정해진 짧은 메시지 작성이라 결정성은 약하게 풀어두고
    (temperature 0.3) 자연스러운 문장이 나오도록 한다.
    JSON 강제는 사용하지 않는다(평문 메시지가 출력 그 자체).
    """
    return LLMConfig(
        model=TEXT_MODEL,
        temperature=0.3,
        top_p=0.9,
        num_ctx=4096,
    )


def get_toxic_detector_config() -> LLMConfig:
    """독소조항 탐지용 JSON 출력 고정 설정.

    커스텀 모델(`TOXIC_DETECTOR_MODEL`)이 등록돼 있으면 그쪽을 우선 사용하고,
    없으면 호출 측에서 fallback으로 `TEXT_MODEL`을 쓰도록 한다.
    JSON 출력을 강제하고 temperature를 낮춰 출력 형식을 안정화한다.
    """
    return LLMConfig(
        model=TOXIC_DETECTOR_MODEL,
        # 결정성을 최대화. 같은 계약서에 호출마다 다른 독소조항 목록이 나오면
        # 디버깅·평가가 불가능해서 temperature는 0으로 둔다.
        temperature=0.0,
        top_p=0.9,
        # 시스템 프롬프트(~3k자) + 참조 자료(~8.6k자) + 계약서(~3k자)가
        # 한 컨텍스트에 들어가야 한다. 8192면 잘려서 모델이 스키마를 못 본다.
        num_ctx=16384,
        seed=42,
        format="json",
    )
