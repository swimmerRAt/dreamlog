"""주소 기반 공공데이터 조회 (건축물대장 + 실거래가)."""
import re
from typing import Optional
from pydantic import BaseModel
from .loader import get_building_data, get_trade_data


# ── Pydantic 모델 ──────────────────────────────────────────────────────────────

class BuildingInfo(BaseModel):
    address: str
    road_address: str
    purpose: str        # 주용도코드명 (예: "단독주택")
    structure: str      # 구조코드명
    floors_above: int   # 지상층수
    approval_date: str  # 사용승인일 (정규화 문자열)
    land_area: float    # 대지면적(㎡)
    total_area: float   # 연면적(㎡)
    status: str         # "정상" 고정 (DB 등록 사실 자체가 정상 의미)


class TradeInfo(BaseModel):
    complex_name: str
    trade_type: str     # 전세 | 월세
    area: str           # 전용면적(㎡)
    deposit: str        # 보증금(만원)
    monthly_rent: str   # 월세금(만원) — 전세면 "0"
    contract_date: str  # YYYYMM
    floor: str
    road_address: str


class PublicDataResult(BaseModel):
    found: bool = False
    building: Optional[BuildingInfo] = None
    recent_trades: list[TradeInfo] = []


# ── 주소 정규화 및 매칭 ────────────────────────────────────────────────────────

def _normalize(addr: str) -> str:
    addr = addr.strip()
    addr = re.sub(r"번지$", "", addr)          # 말미 "번지" 제거
    addr = re.sub(r"[^\w\s\-]", " ", addr)    # 특수문자 → 공백
    addr = re.sub(r"\s+", " ", addr)
    return addr.strip()


def _extract_lot(addr: str) -> tuple[str, str]:
    """(동명, 번지) 추출. e.g. '덕진구 진북동 124-83' → ('진북동', '124-83')"""
    normalized = _normalize(addr)
    # 번지: 숫자(-)숫자 또는 숫자만
    m = re.search(r"(\d+(?:-\d+)?)$", normalized)
    if not m:
        return "", normalized
    lot = m.group(1)
    prefix = normalized[: m.start()].strip().split()
    dong = prefix[-1] if prefix else ""
    return dong, lot


def _matches(query: str, candidate: str) -> bool:
    """query 주소가 candidate 주소와 같은 건물을 가리키는지 판단."""
    q = _normalize(query)
    c = _normalize(candidate)

    if not q:
        return False

    # 직접 포함
    if q in c:
        return True

    # 동명 + 번지 매칭
    q_dong, q_lot = _extract_lot(q)
    c_dong, c_lot = _extract_lot(c)

    if q_lot and q_dong and q_lot == c_lot and q_dong == c_dong:
        return True

    return False


# ── 변환 헬퍼 ─────────────────────────────────────────────────────────────────

def _to_float(val: str) -> float:
    try:
        return float(val.replace(",", "").strip())
    except (ValueError, AttributeError):
        return 0.0


def _to_int(val: str) -> int:
    try:
        return int(str(val).strip())
    except (ValueError, AttributeError):
        return 0


def _format_date(raw: str) -> str:
    """'19770405' → '1977년 04월 05일'"""
    raw = raw.strip()
    if len(raw) == 8 and raw.isdigit():
        return f"{raw[:4]}년 {raw[4:6]}월 {raw[6:]}일"
    return raw


# ── 핵심 조회 함수 ─────────────────────────────────────────────────────────────

def lookup_public_data(address: str) -> PublicDataResult:
    """LLM 추출 주소로 CSV 검색 후 결과 반환."""
    if not address or not address.strip():
        return PublicDataResult(found=False)

    building = _find_building(address)
    trades = _find_trades(address)

    return PublicDataResult(
        found=building is not None or len(trades) > 0,
        building=building,
        recent_trades=trades[:3],
    )


def _find_building(address: str) -> Optional[BuildingInfo]:
    for row in get_building_data():
        land_addr = row.get("대지위치", "")
        road_addr = row.get("도로명대지위치", "")

        if _matches(address, land_addr) or (road_addr and _matches(address, road_addr)):
            return BuildingInfo(
                address=land_addr,
                road_address=road_addr,
                purpose=row.get("주용도코드명", ""),
                structure=row.get("구조코드명", ""),
                floors_above=_to_int(row.get("지상층수", "0")),
                approval_date=_format_date(row.get("사용승인일", "")),
                land_area=_to_float(row.get("대지면적(㎡)", "0")),
                total_area=_to_float(row.get("연면적(㎡)", "0")),
                status="정상",
            )
    return None


def _find_trades(address: str) -> list[TradeInfo]:
    results: list[TradeInfo] = []
    for row in get_trade_data():
        # 실거래가: 시군구(동명 포함) + 번지 조합
        sigungu = row.get("시군구", "")
        lot = row.get("번지", "")
        candidate = f"{sigungu} {lot}".strip()

        if _matches(address, candidate):
            results.append(TradeInfo(
                complex_name=row.get("단지명", ""),
                trade_type=row.get("전월세구분", ""),
                area=row.get("전용면적(㎡)", ""),
                deposit=row.get("보증금(만원)", ""),
                monthly_rent=row.get("월세금(만원)", "0"),
                contract_date=row.get("계약년월", ""),
                floor=row.get("층", ""),
                road_address=row.get("도로명", ""),
            ))

    # 최신 계약 순 정렬
    results.sort(key=lambda t: t.contract_date, reverse=True)
    return results
