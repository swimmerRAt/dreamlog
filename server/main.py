from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from database import init_db
from auth.router import router as auth_router
from contract.router import router as contract_router
from llm_core import get_client
from public_data import load_all as load_public_data


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    load_public_data()
    ollama_ok = await get_client().health()
    if not ollama_ok:
        print("⚠️  경고: Ollama 서버에 연결할 수 없습니다. 'ollama serve'를 실행했는지 확인해주세요.")
    else:
        print("✅ Ollama 연결 확인")
    yield

    
app = FastAPI(
    title="전월세 계약서 독소조항 탐지 API",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(contract_router)


@app.get("/health")
async def health():
    ollama_ok = await get_client().health()
    return {
        "status": "ok",
        "ollama": "connected" if ollama_ok else "disconnected",
    }
