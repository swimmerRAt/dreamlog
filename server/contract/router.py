from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Annotated, List, Optional
from datetime import datetime
import json
from database import get_db, Analysis, User
from auth.utils import get_current_user
from ocr import extract_text
from llm import analyze as llm_analyze
from public_data import lookup_public_data, PublicDataResult

router = APIRouter(prefix="/contract", tags=["contract"])

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "image/heic"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB


class ToxicClause(BaseModel):
    clause: str
    reason: str
    severity: str
    recommendation: str


class AnalysisResponse(BaseModel):
    id: int
    address: str
    risk_level: str
    summary: str
    toxic_clauses: List[ToxicClause]
    original_text: str
    created_at: datetime
    public_data: Optional[PublicDataResult] = None

    class Config:
        from_attributes = True


class AnalysisListItem(BaseModel):
    id: int
    address: str
    risk_level: str
    summary: str
    created_at: datetime

    class Config:
        from_attributes = True


class AnalyzeTextRequest(BaseModel):
    text: str


DbDep = Annotated[Session, Depends(get_db)]
UserDep = Annotated[User, Depends(get_current_user)]


@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_image(
    file: UploadFile = File(...),
    db: DbDep = None,
    current_user: UserDep = None,
):
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(status_code=400, detail="지원하지 않는 파일 형식입니다. (JPEG, PNG, WEBP, HEIC 지원)")

    image_bytes = await file.read()
    if len(image_bytes) > MAX_FILE_SIZE:
        raise HTTPException(status_code=400, detail="파일 크기가 10MB를 초과합니다.")

    try:
        extracted_text = await extract_text(image_bytes)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"OCR 처리 실패: {str(e)}")

    if not extracted_text.strip():
        raise HTTPException(status_code=422, detail="계약서에서 텍스트를 추출할 수 없습니다.")

    try:
        result = await llm_analyze(extracted_text)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"계약서 분석 실패: {str(e)}")

    analysis = Analysis(
        user_id=current_user.id,
        original_text=extracted_text,
        address=result.get("address", ""),
        toxic_clauses=json.dumps(result.get("toxic_clauses", []), ensure_ascii=False),
        summary=result.get("summary", ""),
        risk_level=result.get("risk_level", "알 수 없음"),
    )
    db.add(analysis)
    db.commit()
    db.refresh(analysis)

    return _build_response(analysis)


@router.post("/analyze-text", response_model=AnalysisResponse)
async def analyze_text(
    req: AnalyzeTextRequest,
    db: DbDep = None,
    current_user: UserDep = None,
):
    """OCR을 건너뛰고 텍스트로 바로 독소조항 분석. 통합 테스트·복붙 입력용."""
    text = req.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="텍스트가 비어 있습니다.")

    try:
        result = await llm_analyze(text)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"계약서 분석 실패: {str(e)}")

    analysis = Analysis(
        user_id=current_user.id,
        original_text=text,
        address=result.get("address", ""),
        toxic_clauses=json.dumps(result.get("toxic_clauses", []), ensure_ascii=False),
        summary=result.get("summary", ""),
        risk_level=result.get("risk_level", "알 수 없음"),
    )
    db.add(analysis)
    db.commit()
    db.refresh(analysis)

    return _build_response(analysis)


@router.get("/history", response_model=List[AnalysisListItem])
def get_history(
    db: DbDep = None,
    current_user: UserDep = None,
):
    analyses = (
        db.query(Analysis)
        .filter(Analysis.user_id == current_user.id)
        .order_by(Analysis.created_at.desc())
        .all()
    )
    return analyses


@router.get("/{analysis_id}", response_model=AnalysisResponse)
def get_analysis(
    analysis_id: int,
    db: DbDep = None,
    current_user: UserDep = None,
):
    analysis = db.query(Analysis).filter(
        Analysis.id == analysis_id,
        Analysis.user_id == current_user.id,
    ).first()

    if not analysis:
        raise HTTPException(status_code=404, detail="분석 기록을 찾을 수 없습니다.")

    return _build_response(analysis)


def _build_response(analysis: Analysis) -> dict:
    toxic_clauses = json.loads(analysis.toxic_clauses) if analysis.toxic_clauses else []
    public_data = lookup_public_data(analysis.address or "")
    return {
        "id": analysis.id,
        "address": analysis.address or "",
        "risk_level": analysis.risk_level,
        "summary": analysis.summary,
        "toxic_clauses": toxic_clauses,
        "original_text": analysis.original_text,
        "created_at": analysis.created_at,
        "public_data": public_data.model_dump(),
    }
