"""CSV 파일 로드 및 메모리 캐싱."""
import csv
from pathlib import Path

_DATA_DIR = Path(__file__).parent.parent.parent / "data" / "information"

_building_data: list[dict] = []
_trade_data: list[dict] = []
_loaded = False


def _read_csv(path: Path) -> list[dict]:
    rows = []
    with open(path, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(dict(row))
    return rows


def load_all() -> None:
    global _building_data, _trade_data, _loaded
    if _loaded:
        return

    for f in sorted(_DATA_DIR.glob("*_건축물대장.csv")):
        _building_data.extend(_read_csv(f))

    for f in sorted(_DATA_DIR.glob("실거래가_*.csv")):
        _trade_data.extend(_read_csv(f))

    _loaded = True
    print(
        f"✅ 공공데이터 로드 완료: "
        f"건축물대장 {len(_building_data):,}건 | "
        f"실거래가 {len(_trade_data):,}건"
    )


def get_building_data() -> list[dict]:
    if not _loaded:
        load_all()
    return _building_data


def get_trade_data() -> list[dict]:
    if not _loaded:
        load_all()
    return _trade_data
