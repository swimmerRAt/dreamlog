"""HuggingFace Qwen2-VL 기반 비전 추론 백엔드.

OCR 등 비전 태스크에서 Ollama 대신 로컬 HF 캐시의 Qwen2-VL 모델을 호출한다.
모델·프로세서는 모듈 단위 싱글톤으로 한 번만 로드한다.

환경 변수:
- QWEN_OCR_MODEL: HF 모델 ID (기본: Qwen/Qwen2-VL-2B-Instruct)
- QWEN_OCR_DEVICE: cuda / cuda:0 / cpu (기본: 가능하면 cuda, 아니면 cpu)
- QWEN_OCR_DTYPE: float16 / bfloat16 / float32 (기본: cuda면 float16, cpu면 float32)
"""
from __future__ import annotations

import asyncio
import io
import os
import sys
import threading
from typing import Optional

import torch
from PIL import Image
from transformers import AutoProcessor, Qwen2VLForConditionalGeneration


QWEN_OCR_MODEL: str = os.getenv("QWEN_OCR_MODEL", "Qwen/Qwen2-VL-2B-Instruct")
QWEN_OCR_DEVICE: str = os.getenv("QWEN_OCR_DEVICE", "")
QWEN_OCR_DTYPE: str = os.getenv("QWEN_OCR_DTYPE", "")


def _llm_debug() -> bool:
    return os.getenv("LLM_DEBUG", "").lower() in ("1", "true", "yes")


def _resolve_device() -> str:
    if QWEN_OCR_DEVICE:
        return QWEN_OCR_DEVICE
    return "cuda" if torch.cuda.is_available() else "cpu"


def _resolve_dtype(device: str) -> torch.dtype:
    if QWEN_OCR_DTYPE:
        return {"float16": torch.float16, "bfloat16": torch.bfloat16,
                "float32": torch.float32}[QWEN_OCR_DTYPE]
    return torch.float16 if device.startswith("cuda") else torch.float32


_model: Optional[Qwen2VLForConditionalGeneration] = None
_processor = None
_load_lock = threading.Lock()


def _load() -> tuple[Qwen2VLForConditionalGeneration, object, str]:
    """모델·프로세서를 한 번만 로드하고 캐싱한다."""
    global _model, _processor
    if _model is not None and _processor is not None:
        return _model, _processor, next(_model.parameters()).device.type

    with _load_lock:
        if _model is not None and _processor is not None:
            return _model, _processor, next(_model.parameters()).device.type

        device = _resolve_device()
        dtype = _resolve_dtype(device)
        if _llm_debug():
            print(f"[QWEN] loading model={QWEN_OCR_MODEL} device={device} dtype={dtype}",
                  file=sys.stderr)
        processor = AutoProcessor.from_pretrained(QWEN_OCR_MODEL)
        model = Qwen2VLForConditionalGeneration.from_pretrained(
            QWEN_OCR_MODEL,
            torch_dtype=dtype,
            device_map=device,
        )
        model.eval()
        _model = model
        _processor = processor
        if _llm_debug():
            print(f"[QWEN] loaded device={next(model.parameters()).device}", file=sys.stderr)
        return model, processor, next(model.parameters()).device.type


def _run_generate(
    image_bytes: bytes,
    prompt: str,
    *,
    max_new_tokens: int,
    do_sample: bool,
    temperature: float,
) -> str:
    model, processor, _ = _load()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    messages = [
        {
            "role": "user",
            "content": [
                {"type": "image"},
                {"type": "text", "text": prompt},
            ],
        }
    ]
    text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = processor(text=[text], images=[image], padding=True, return_tensors="pt")
    inputs = inputs.to(model.device)

    gen_kwargs = {"max_new_tokens": max_new_tokens, "do_sample": do_sample}
    if do_sample:
        gen_kwargs["temperature"] = temperature

    with torch.inference_mode():
        out_ids = model.generate(**inputs, **gen_kwargs)

    trimmed = out_ids[:, inputs.input_ids.shape[1]:]
    decoded = processor.batch_decode(
        trimmed, skip_special_tokens=True, clean_up_tokenization_spaces=False
    )[0]

    if _llm_debug():
        print(
            f"[QWEN] generate image_bytes={len(image_bytes)} prompt_chars={len(prompt)} "
            f"out_chars={len(decoded)} max_new_tokens={max_new_tokens}",
            file=sys.stderr,
        )
    return decoded


async def qwen_vision_generate(
    prompt: str,
    image_bytes: bytes,
    *,
    max_new_tokens: int = 2048,
    do_sample: bool = False,
    temperature: float = 0.0,
) -> str:
    """이미지+프롬프트로 Qwen2-VL을 호출한다. 동기 추론을 별도 스레드에서 실행."""
    return await asyncio.to_thread(
        _run_generate,
        image_bytes,
        prompt,
        max_new_tokens=max_new_tokens,
        do_sample=do_sample,
        temperature=temperature,
    )
