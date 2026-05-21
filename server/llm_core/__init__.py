"""LLM 엔진 패키지 (내부 구현).

공개 인터페이스는 상위 llm.py / ocr.py 를 사용한다.
"""
from .config import (
    LLMConfig,
    OLLAMA_BASE_URL,
    OLLAMA_TIMEOUT,
    TEXT_MODEL,
    TOXIC_DETECTOR_MODEL,
    VISION_MODEL,
    get_negotiation_config,
    get_text_config,
    get_toxic_detector_config,
    get_vision_config,
)
from .client import OllamaClient, OllamaError, get_client
from .modelfile import (
    TOXIC_DETECTOR_SYSTEM_PROMPT,
    build_model_spec,
    build_modelfile,
    create_toxic_detector,
    load_references_from_dir,
)
from .tasks import (
    ADDRESS_EXTRACT_PROMPT,
    CONTRACT_OCR_PROMPT,
    IMAGE_DESCRIPTION_PROMPT,
    NEGOTIATION_SCRIPT_PROMPT,
    analyze_contract,
    describe_image,
    extract_contract_address,
    generate_negotiation_scripts,
    ocr_contract_image,
)

__all__ = [
    "LLMConfig",
    "OLLAMA_BASE_URL",
    "OLLAMA_TIMEOUT",
    "TEXT_MODEL",
    "TOXIC_DETECTOR_MODEL",
    "VISION_MODEL",
    "OllamaClient",
    "OllamaError",
    "ADDRESS_EXTRACT_PROMPT",
    "CONTRACT_OCR_PROMPT",
    "IMAGE_DESCRIPTION_PROMPT",
    "NEGOTIATION_SCRIPT_PROMPT",
    "TOXIC_DETECTOR_SYSTEM_PROMPT",
    "analyze_contract",
    "build_model_spec",
    "build_modelfile",
    "create_toxic_detector",
    "describe_image",
    "extract_contract_address",
    "generate_negotiation_scripts",
    "get_client",
    "get_negotiation_config",
    "get_text_config",
    "get_toxic_detector_config",
    "get_vision_config",
    "load_references_from_dir",
    "ocr_contract_image",
]
