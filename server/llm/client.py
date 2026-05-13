"""Ollama HTTP API 클라이언트.

`/api/generate`, `/api/chat`, `/api/tags`, `/api/pull`, `/api/create`를 감싼다.
모든 호출은 비동기이며 `httpx.AsyncClient`를 사용한다.
"""
from __future__ import annotations

import base64
import json
from typing import Any, AsyncIterator, Optional

import httpx

from .config import LLMConfig, OLLAMA_BASE_URL, OLLAMA_TIMEOUT


class OllamaError(RuntimeError):
    """Ollama 호출 중 발생한 오류."""


class OllamaClient:
    def __init__(
        self,
        base_url: str = OLLAMA_BASE_URL,
        timeout: float = OLLAMA_TIMEOUT,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def _client(self) -> httpx.AsyncClient:
        return httpx.AsyncClient(base_url=self.base_url, timeout=self.timeout)

    async def health(self) -> bool:
        try:
            async with self._client() as client:
                res = await client.get("/api/tags", timeout=5.0)
                return res.status_code == 200
        except Exception:
            return False

    async def list_models(self) -> list[dict[str, Any]]:
        async with self._client() as client:
            res = await client.get("/api/tags")
            res.raise_for_status()
            return res.json().get("models", [])

    async def has_model(self, name: str) -> bool:
        models = await self.list_models()
        # Ollama는 태그를 "name:tag" 형태로 반환하지만 기본 태그는 "latest"
        candidates = {name, f"{name}:latest"}
        return any(m.get("name") in candidates or m.get("model") in candidates for m in models)

    async def pull(self, name: str) -> None:
        """모델이 로컬에 없으면 다운로드. 진행 상황은 스트리밍으로 받지만 결과만 반환."""
        async with self._client() as client:
            async with client.stream(
                "POST",
                "/api/pull",
                json={"name": name, "stream": True},
                timeout=None,
            ) as res:
                res.raise_for_status()
                async for line in res.aiter_lines():
                    if not line:
                        continue
                    try:
                        chunk = json.loads(line)
                    except json.JSONDecodeError:
                        continue
                    if chunk.get("error"):
                        raise OllamaError(chunk["error"])

    async def ensure_model(self, name: str) -> None:
        """모델이 없으면 pull로 가져온다."""
        if not await self.has_model(name):
            await self.pull(name)

    async def create_model(
        self,
        name: str,
        *,
        from_model: str,
        system: Optional[str] = None,
        parameters: Optional[dict[str, Any]] = None,
    ) -> None:
        """구조화된 페이로드로 커스텀 모델을 등록한다.

        Ollama 신 API(`/api/create`)는 `modelfile` 문자열 대신
        `from`/`system`/`parameters` 등 필드를 받는다.
        """
        payload: dict[str, Any] = {
            "model": name,
            "from": from_model,
            "stream": True,
        }
        if system is not None:
            payload["system"] = system
        if parameters:
            payload["parameters"] = parameters
        async with self._client() as client:
            async with client.stream(
                "POST",
                "/api/create",
                json=payload,
                timeout=None,
            ) as res:
                res.raise_for_status()
                async for line in res.aiter_lines():
                    if not line:
                        continue
                    try:
                        chunk = json.loads(line)
                    except json.JSONDecodeError:
                        continue
                    if chunk.get("error"):
                        raise OllamaError(chunk["error"])

    async def delete_model(self, name: str) -> None:
        async with self._client() as client:
            res = await client.request("DELETE", "/api/delete", json={"name": name})
            res.raise_for_status()

    async def generate(
        self,
        prompt: str,
        config: LLMConfig,
        *,
        images: Optional[list[bytes]] = None,
    ) -> str:
        """단발성 텍스트 생성. 이미지가 있으면 base64로 첨부."""
        image_b64 = [base64.b64encode(b).decode("utf-8") for b in images] if images else None
        payload = config.to_payload(prompt, images=image_b64)
        async with self._client() as client:
            res = await client.post("/api/generate", json=payload)
            res.raise_for_status()
            return res.json()["response"]

    async def generate_json(
        self,
        prompt: str,
        config: LLMConfig,
        *,
        images: Optional[list[bytes]] = None,
    ) -> dict[str, Any]:
        """JSON 출력을 파싱해 dict로 반환. format을 강제로 json으로 설정."""
        cfg = config.merged(format="json")
        raw = await self.generate(prompt, cfg, images=images)
        try:
            return json.loads(raw)
        except json.JSONDecodeError as e:
            raise OllamaError(f"JSON 파싱 실패: {e}\nraw: {raw[:500]}") from e

    async def chat(
        self,
        messages: list[dict[str, Any]],
        config: LLMConfig,
    ) -> str:
        """`/api/chat` 호출. messages는 [{role, content, images?}, ...] 형식."""
        payload: dict[str, Any] = {
            "model": config.model,
            "messages": messages,
            "stream": False,
            "options": config.to_options(),
        }
        if config.format:
            payload["format"] = config.format
        async with self._client() as client:
            res = await client.post("/api/chat", json=payload)
            res.raise_for_status()
            return res.json()["message"]["content"]

    async def stream_generate(
        self,
        prompt: str,
        config: LLMConfig,
    ) -> AsyncIterator[str]:
        """토큰 스트리밍. 학습 데이터 평가 등에 유용."""
        payload = config.to_payload(prompt)
        payload["stream"] = True
        async with self._client() as client:
            async with client.stream("POST", "/api/generate", json=payload) as res:
                res.raise_for_status()
                async for line in res.aiter_lines():
                    if not line:
                        continue
                    try:
                        chunk = json.loads(line)
                    except json.JSONDecodeError:
                        continue
                    token = chunk.get("response")
                    if token:
                        yield token


_default_client: Optional[OllamaClient] = None


def get_client() -> OllamaClient:
    """전역에서 재사용할 기본 클라이언트."""
    global _default_client
    if _default_client is None:
        _default_client = OllamaClient()
    return _default_client
