# LLM 모듈 구조

전월세 계약서 이미지를 OCR → 독소조항 분석으로 처리하는 LLM 관련 코드. Ollama를 통해 로컬에서 비전·텍스트 모델을 호출하며, 독소조항 탐지용 커스텀 모델(`dreamlog-toxic-detector`)을 학습 자료와 함께 등록할 수 있다.

## 디렉토리 구조

```
dreamlog/
├── data/
│   ├── contract_ex1.txt           # 분석 검증용 예시 계약서 (전세 표준계약서 + 독소조항 혼합)
│   └── contract_rule.xlsx         # 독소조항 학습 자료 (라벨링된 조항 · 법 조문)
└── server/
    ├── main.py                    # FastAPI 진입점 — lifespan에서 Ollama health 체크
    ├── .env.example               # Ollama 모델·서버 URL 환경변수 템플릿
    ├── requirements.txt           # openpyxl 추가 (xlsx 학습 자료 파싱용)
    ├── llm_test.py                # 학습-검증 CLI: rule xlsx로 모델 재등록 후 ex1.txt 분석
    ├── contract/
    │   └── router.py              # /contract/analyze — OCR + analyze_contract 호출
    └── llm/                       # ← 새 모듈
        ├── __init__.py            # 모듈 진입점 (외부에 공개할 심볼 묶음)
        ├── config.py              # LLMConfig 데이터클래스 + 용도별 프리셋
        ├── client.py              # Ollama HTTP API 비동기 래퍼
        ├── tasks.py               # 고수준 태스크 (OCR / 계약서 분석)
        └── modelfile.py           # 커스텀 모델 빌더 (시스템 프롬프트 + 참조 자료 주입)
```

## `server/llm/` — 새로 만든 LLM 모듈

### `__init__.py`
모듈 진입점. 외부(`server/main.py`, `server/contract/router.py`, `server/llm_test.py`)에서 사용할 함수·상수를 한 번에 export한다.

```python
from llm import analyze_contract, ocr_contract_image, get_client, create_toxic_detector
```

### `config.py`
- `LLMConfig` 데이터클래스: Ollama `/api/generate`·`/api/chat` 호출에 들어가는 모든 파라미터(모델명·temperature·top_p·num_ctx·format 등)를 한 객체로 묶음. `to_payload()` / `to_options()` / `merged()` 헬퍼 제공.
- 용도별 프리셋:
  - `get_vision_config()` — OCR용. temperature=0 (정확한 텍스트 추출).
  - `get_text_config()` — 일반 텍스트 생성.
  - `get_toxic_detector_config()` — 독소조항 탐지. `format="json"` 강제, 낮은 temperature.
- 모델명·서버 URL·타임아웃은 환경변수(`OLLAMA_BASE_URL`, `VISION_MODEL`, `TEXT_MODEL`, `TOXIC_DETECTOR_MODEL`)로 오버라이드 가능.

### `client.py`
Ollama HTTP API 비동기 래퍼 (`httpx.AsyncClient` 사용).

| 메서드 | 용도 |
| --- | --- |
| `health()` | `/api/tags` ping으로 서버 상태 확인 |
| `list_models()` / `has_model()` | 등록된 모델 조회 |
| `pull()` / `ensure_model()` | 모델이 없으면 다운로드 |
| `create_model(name, *, from_model, system, parameters)` | 신 `/api/create` API로 커스텀 모델 등록. **구버전의 `modelfile` 문자열 필드 대신 구조화된 페이로드 사용** |
| `delete_model()` | 등록 해제 |
| `generate()` / `generate_json()` | `/api/generate` 단발 호출. JSON 출력은 자동 파싱 |
| `chat()` | `/api/chat` 호출 (멀티턴) |
| `stream_generate()` | 토큰 스트리밍 (평가용) |

`get_client()`로 프로세스 전역 클라이언트를 재사용한다.

### `tasks.py`
고수준 태스크 두 개:

- **`ocr_contract_image(image_bytes)`** — 계약서 이미지 → 텍스트. 비전 모델(`llava`)에 `CONTRACT_OCR_PROMPT`를 함께 보낸다.
- **`analyze_contract(contract_text)`** — 계약서 텍스트 → 독소조항 JSON. 커스텀 모델(`TOXIC_DETECTOR_MODEL`)이 등록돼 있으면 그쪽을 사용하고, 없으면 일반 텍스트 모델에 전체 분석 프롬프트를 폴백으로 주입한다.

반환 JSON 스키마:
```json
{
  "risk_level": "높음 | 중간 | 낮음",
  "summary": "한 줄 요약",
  "toxic_clauses": [
    { "clause": "...", "reason": "...", "severity": "...", "recommendation": "..." }
  ]
}
```

### `modelfile.py`
독소조항 탐지 커스텀 모델 빌더.

- **`TOXIC_DETECTOR_SYSTEM_PROMPT`** — 주택임대차보호법 기반 판단 원칙과 출력 JSON 스키마를 정의한 시스템 프롬프트.
- **`build_model_spec(...)`** — Ollama 신 `/api/create` API에 보낼 dict 페이로드(`from` / `system` / `parameters`)를 만든다. 참조 자료(`references`)는 시스템 프롬프트 하단에 `[참조 자료]` 블록으로 덧붙인다.
- **`build_modelfile(...)`** — 동일 내용을 사람이 읽을 수 있는 Modelfile 문자열로 만든다. 등록에는 쓰지 않고 `--save-modelfile` 같은 디버깅·저장용 표현.
- **`load_references_from_dir(dir)`** — 디렉토리 안의 `.txt`/`.md`를 참조 자료 리스트로 로드.
- **`create_toxic_detector(*, name, base_model, references_dir, extra_references)`** — 베이스 모델을 보장(`ensure_model`)한 뒤 spec을 만들어 `client.create_model()`로 등록. 반환값은 디버깅용 Modelfile 문자열.

## `server/llm_test.py` — 학습-검증 CLI

`data/contract_rule.xlsx` → 커스텀 모델 재등록 → `data/contract_ex1.txt` 분석 한 번에 돌리는 스크립트.

흐름:
1. `openpyxl`로 xlsx를 읽어 3개 학습 시트를 텍스트 블록으로 변환
   - `계약서_독소조항_데이터셋` (라벨링된 조항 예시)
   - `독소조항_수정안_페어` (원문 ↔ 수정안)
   - `주택임대차보호법_조문_위험도라벨` (법조문 + 위험도)
   - (출처 정리 시트는 학습에 불필요해서 제외)
2. `create_toxic_detector(extra_references=...)`로 시스템 프롬프트에 학습 자료를 주입한 커스텀 모델 등록
3. `contract_ex1.txt` 텍스트를 `analyze_contract()`로 분석해 위험도·요약·독소조항 리스트 출력

옵션:
```
python3 llm_test.py                       # 기본 경로 사용
python3 llm_test.py --rule ... --contract ...
python3 llm_test.py --skip-train          # 모델 재등록 생략, 분석만
python3 llm_test.py --save-json out.json
python3 llm_test.py --save-modelfile mf.txt
```

## `server/` 쪽 통합 지점

- **`main.py`** — lifespan 훅에서 `get_client().health()`로 Ollama 연결 상태를 체크하고 로그 출력.
- **`contract/router.py`** — `POST /contract/analyze`: 업로드된 이미지를 `ocr_contract_image()`로 텍스트화한 뒤 `analyze_contract()`에 넘기고, 결과를 `Analysis` 테이블에 저장. (기존 `contract/analyzer.py`는 제거됨 — 로직이 `llm/tasks.py`로 이동)
- **`.env.example`** — `OLLAMA_BASE_URL`, `OLLAMA_TIMEOUT`, `VISION_MODEL`, `TEXT_MODEL`, `TOXIC_DETECTOR_MODEL` 키 추가.
- **`requirements.txt`** — xlsx 파싱을 위해 `openpyxl==3.1.5` 추가.

## 빠르게 실행해보기

```bash
# 1) Ollama 서버 기동 (별도 터미널)
ollama serve

# 2) 베이스 모델 사전 다운로드 (선택 — create_toxic_detector가 자동으로 pull해줌)
ollama pull llama3.2
ollama pull llava

# 3) 학습-검증 한 번에
cd server
python3 llm_test.py
```
