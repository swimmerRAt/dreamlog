# 서버·LLM 구현 변수 정리

`server/`(FastAPI)와 `server/llm/`(Ollama 연동) 모듈에서 실제로 사용하는 변수명을 한곳에 모은 문서입니다. 프론트엔드·백엔드 담당자가 API 요청/응답 필드, DB 컬럼명, LLM 설정값을 빠르게 매칭할 수 있도록 정리했습니다.

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | [server/main.py](server/main.py), [server/database.py](server/database.py), [server/auth/router.py](server/auth/router.py), [server/auth/utils.py](server/auth/utils.py), [server/contract/router.py](server/contract/router.py), [server/ocr_image.py](server/ocr_image.py), [server/llm/](server/llm/) 하위 모든 파일(`qwen_ocr.py` 포함) |
| 기준 시점 | 현재 `main` 기준 구현 (OCR은 HuggingFace Qwen2-VL, 분석은 Ollama) |
| 응답 포맷 | FastAPI 기본 (성공 시 모델 객체 그대로, 실패 시 `{"detail": "..."}`) |

---

## 1. 인증(Auth) 관련 변수

### 1-1. 회원가입 `POST /auth/register`
[server/auth/router.py:11](server/auth/router.py#L11)

**요청 body (`RegisterRequest`)**

| 변수명 | 타입 | 의미 | 비고 |
| --- | --- | --- | --- |
| `username` | `string` | 사용자명 | 중복 불가 |
| `email` | `string (EmailStr)` | 이메일 | 중복 불가, 형식 검증 |
| `password` | `string` | 비밀번호 평문 | 서버에서 bcrypt 해시 후 저장 |

**응답 body (`UserResponse`)**

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `id` | `int` | 사용자 PK |
| `username` | `string` | 사용자명 |
| `email` | `string` | 이메일 |

### 1-2. 로그인 `POST /auth/login`
[server/auth/router.py:44](server/auth/router.py#L44)

**요청 (OAuth2PasswordRequestForm, `application/x-www-form-urlencoded`)**

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `username` | `string` | 사용자명 (이메일 아님) |
| `password` | `string` | 비밀번호 평문 |

**응답 body**

| 변수명 | 타입 | 의미 | 비고 |
| --- | --- | --- | --- |
| `access_token` | `string` | JWT 토큰 | 이후 모든 보호 API의 `Authorization: Bearer <token>` 헤더에 사용 |
| `token_type` | `string` | 항상 `"bearer"` | OAuth2 표준 |

### 1-3. 내 정보 조회 `GET /auth/me`
[server/auth/router.py:54](server/auth/router.py#L54)

**헤더**: `Authorization: Bearer <access_token>`
**응답**: `UserResponse` (1-1 응답과 동일)

### 1-4. 인증 내부 설정 변수
[server/auth/utils.py](server/auth/utils.py)

| 변수명 | 타입 | 의미 | 기본값 | 출처 |
| --- | --- | --- | --- | --- |
| `SECRET_KEY` | `string` | JWT 서명 키 | `fallback-dev-secret-key` | `.env` |
| `ALGORITHM` | `string` | JWT 서명 알고리즘 | `HS256` | `.env` |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | `int` | 토큰 만료 분 | `60` | `.env` |
| JWT payload `sub` | `string` | 사용자 id (문자열로 인코딩) | — | `create_access_token` |
| JWT payload `exp` | `datetime` | 만료 시각 (UTC) | — | `create_access_token` |

---

## 2. 계약서 분석(Contract) 관련 변수

### 2-1. 이미지 업로드 분석 `POST /contract/analyze`
[server/contract/router.py:50](server/contract/router.py#L50)

**요청** (`multipart/form-data`, 인증 필요)

| 변수명 | 타입 | 의미 | 제약 |
| --- | --- | --- | --- |
| `file` | `UploadFile` | 계약서 이미지 | MIME: `image/jpeg`, `image/png`, `image/webp`, `image/heic` / 최대 10MB |

**응답 body (`AnalysisResponse`)**

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `id` | `int` | 분석 기록 PK |
| `risk_level` | `string` | 전체 위험도. `"높음" / "중간" / "낮음" / "알 수 없음"` |
| `summary` | `string` | 계약서 한 줄 요약 (한국어) |
| `toxic_clauses` | `array<ToxicClause>` | 독소조항 목록 (아래 표 참고) |
| `original_text` | `string` | OCR로 추출된 원문 텍스트 |
| `created_at` | `datetime` | 분석 시각 (UTC) |

**`ToxicClause` 객체**
[server/contract/router.py:17](server/contract/router.py#L17)

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `clause` | `string` | 문제 조항 원문 인용 |
| `reason` | `string` | 왜 세입자에게 불리한지 / 근거 법령 |
| `severity` | `string` | 항목별 위험도. `"높음" / "중간" / "낮음"` |
| `recommendation` | `string` | 세입자가 취해야 할 조치 |

### 2-2. 텍스트 직접 분석 `POST /contract/analyze-text`
[server/contract/router.py:90](server/contract/router.py#L90)

OCR을 건너뛰고 텍스트만으로 분석. 통합 테스트·복붙 입력용.

**요청 body (`AnalyzeTextRequest`)**

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `text` | `string` | 계약서 텍스트 (공백만 있으면 400) |

**응답**: `AnalysisResponse` (2-1과 동일)

### 2-3. 분석 이력 목록 `GET /contract/history`
[server/contract/router.py:120](server/contract/router.py#L120)

**응답**: `array<AnalysisListItem>` (최신순)

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `id` | `int` | 분석 기록 PK |
| `risk_level` | `string` | 위험도 |
| `summary` | `string` | 한 줄 요약 |
| `created_at` | `datetime` | 분석 시각 |

> 목록 응답에는 `toxic_clauses`, `original_text`가 포함되지 않음. 상세는 `/contract/{analysis_id}`로 조회.

### 2-4. 분석 상세 조회 `GET /contract/{analysis_id}`
[server/contract/router.py:134](server/contract/router.py#L134)

**경로 파라미터**: `analysis_id` (`int`)
**응답**: `AnalysisResponse` (2-1과 동일). 본인 소유 기록이 아니면 404.

### 2-5. 업로드 제약 상수
[server/contract/router.py:13](server/contract/router.py#L13)

| 변수명 | 값 | 의미 |
| --- | --- | --- |
| `ALLOWED_TYPES` | `{"image/jpeg", "image/png", "image/webp", "image/heic"}` | 허용 MIME |
| `MAX_FILE_SIZE` | `10 * 1024 * 1024` (10MB) | 업로드 최대 크기 |

---

## 3. DB 모델 변수 (SQLAlchemy)
[server/database.py](server/database.py)

### `User` 테이블 (`users`)

| 컬럼 | 타입 | 의미 | 제약 |
| --- | --- | --- | --- |
| `id` | `Integer` | PK | auto-increment |
| `username` | `String` | 사용자명 | unique, not null |
| `email` | `String` | 이메일 | unique, not null |
| `hashed_password` | `String` | bcrypt 해시 | not null |
| `created_at` | `DateTime` | 생성 시각 (UTC) | 기본값 자동 |

### `Analysis` 테이블 (`analyses`)

| 컬럼 | 타입 | 의미 |
| --- | --- | --- |
| `id` | `Integer` | PK |
| `user_id` | `Integer` | `users.id` 외래키 |
| `original_text` | `Text` | OCR 원문 |
| `toxic_clauses` | `Text` | `ToxicClause[]`의 JSON 문자열 (ensure_ascii=False) |
| `summary` | `Text` | 한 줄 요약 |
| `risk_level` | `String` | 전체 위험도 |
| `created_at` | `DateTime` | 생성 시각 (UTC) |

> 응답으로 나갈 때 `toxic_clauses`는 `_build_response`에서 JSON 문자열을 파싱해 배열로 풀어줌. 프론트는 항상 배열로 받음.

### DB 연결 변수

| 변수명 | 의미 | 기본값 | 출처 |
| --- | --- | --- | --- |
| `DATABASE_URL` | DB 접속 URL | `sqlite:///./dreamlog.db` | `.env` |
| `engine` | SQLAlchemy 엔진 | — | `create_engine` |
| `SessionLocal` | 세션 팩토리 | — | `sessionmaker` |
| `Base` | 모델 베이스 | — | `declarative_base` |

---

## 4. LLM 설정 변수
[server/llm/config.py](server/llm/config.py), [server/llm/qwen_ocr.py](server/llm/qwen_ocr.py)

### 환경 변수 (`.env`)

| 변수명 | 타입 | 의미 | 기본값 | 출처 |
| --- | --- | --- | --- | --- |
| `OLLAMA_BASE_URL` | `string` | Ollama 서버 주소 | `http://localhost:11434` | `config.py` |
| `OLLAMA_TIMEOUT` | `float` | 요청 타임아웃(초) | `120` | `config.py` |
| `VISION_MODEL` | `string` | Ollama 비전 모델 태그 (현재 OCR은 Qwen2-VL 사용으로 미적용, 폴백 시 사용) | `llava` | `config.py` |
| `TEXT_MODEL` | `string` | 일반 텍스트 모델 태그 | `llama3.2` | `config.py` |
| `TOXIC_DETECTOR_MODEL` | `string` | 독소조항 탐지 커스텀 모델 태그 | `dreamlog-toxic-detector` | `config.py` |
| `QWEN_OCR_MODEL` | `string` | OCR용 HuggingFace 모델 ID | `Qwen/Qwen2-VL-2B-Instruct` | `qwen_ocr.py` |
| `QWEN_OCR_DEVICE` | `string` | 추론 디바이스 (`cuda` / `cuda:0` / `cpu`) | 자동 (cuda 가능 시 cuda) | `qwen_ocr.py` |
| `QWEN_OCR_DTYPE` | `string` | 가중치 dtype (`float16` / `bfloat16` / `float32`) | 자동 (cuda면 float16) | `qwen_ocr.py` |
| `LLM_DEBUG` | `string` | `1`이면 LLM/OCR 호출 메타데이터를 stderr로 출력 | `0` | `client.py`, `qwen_ocr.py` |

### `LLMConfig` 데이터클래스
[server/llm/config.py:25](server/llm/config.py#L25)

Ollama `generate` / `chat` 호출에 전달할 설정 묶음.

| 필드 | 타입 | 기본값 | 의미 |
| --- | --- | --- | --- |
| `model` | `string` | — | Ollama 모델 태그 (필수) |
| `temperature` | `float` | `0.2` | 무작위성 |
| `top_p` | `float` | `0.9` | nucleus 샘플링 |
| `top_k` | `int` | `40` | top-k 샘플링 |
| `num_ctx` | `int` | `4096` | 컨텍스트 길이 |
| `num_predict` | `int` | `-1` | 생성 토큰 수 (-1 = 무제한) |
| `repeat_penalty` | `float` | `1.1` | 반복 페널티 |
| `seed` | `int \| None` | `None` | 재현성을 위한 시드 |
| `stop` | `list[str]` | `[]` | 종료 토큰 목록 |
| `system` | `string \| None` | `None` | 시스템 프롬프트 |
| `format` | `string \| None` | `None` | `"json"`이면 JSON 출력 강제 |

### 프리셋 함수

| 함수 | 반환 모델 | 주요 설정 | 용도 |
| --- | --- | --- | --- |
| `get_vision_config()` | `VISION_MODEL` | `temperature=0.0, top_p=1.0` | Ollama 비전 호출용. 현재 OCR은 `qwen_ocr.qwen_vision_generate()`로 우회하므로 미사용 |
| `get_text_config()` | `TEXT_MODEL` | `temperature=0.3, top_p=0.9` | 일반 텍스트 생성 |
| `get_toxic_detector_config()` | `TOXIC_DETECTOR_MODEL` | `temperature=0.1, num_ctx=8192, format="json"` | 독소조항 탐지 (JSON 강제) |

---

## 5. Ollama 클라이언트 API
[server/llm/client.py](server/llm/client.py)

### `OllamaClient` 메서드

| 메서드 | 매핑 Ollama 엔드포인트 | 의미 |
| --- | --- | --- |
| `health()` | `GET /api/tags` | 서버 연결 확인 (200이면 True) |
| `list_models()` | `GET /api/tags` | 설치된 모델 목록 |
| `has_model(name)` | `GET /api/tags` | 특정 모델 설치 여부 (`name`, `name:latest` 모두 확인) |
| `pull(name)` | `POST /api/pull` | 모델 다운로드 |
| `ensure_model(name)` | (조건부 `pull`) | 없으면 자동 pull |
| `create_model(name, from_model, system, parameters)` | `POST /api/create` | 커스텀 모델 등록 |
| `delete_model(name)` | `DELETE /api/delete` | 모델 삭제 |
| `generate(prompt, config, images=None)` | `POST /api/generate` | 단발성 텍스트 생성 (응답: `string`) |
| `generate_json(prompt, config, images=None)` | `POST /api/generate` | JSON 출력 강제 후 `dict` 반환 |
| `chat(messages, config)` | `POST /api/chat` | 멀티턴 메시지 호출 |
| `stream_generate(prompt, config)` | `POST /api/generate` (stream) | 토큰 스트리밍 (`AsyncIterator[str]`) |

### Ollama 요청/응답 키 (`LLMConfig.to_payload`)

| 키 | 의미 |
| --- | --- |
| `model` | 모델 태그 |
| `prompt` | 사용자 프롬프트 |
| `stream` | 항상 `False` (단발), `stream_generate`에서만 `True` |
| `options` | `temperature`, `top_p`, `top_k`, `num_ctx`, `num_predict`, `repeat_penalty`, `seed?`, `stop?` |
| `system` | 시스템 프롬프트 (옵션) |
| `format` | `"json"` (옵션) |
| `images` | base64 인코딩된 이미지 배열 (비전 모델용) |

### 클라이언트 전역 인스턴스

| 함수 | 의미 |
| --- | --- |
| `get_client()` | 프로세스 내 재사용되는 기본 `OllamaClient` 싱글톤 반환 |
| `OllamaError` | 클라이언트 호출 중 예외 클래스 (JSON 파싱 실패, pull 에러 등) |

---

## 6. LLM 태스크 함수
[server/llm/tasks.py](server/llm/tasks.py)

### 공개 함수

| 함수 | 시그니처 | 의미 |
| --- | --- | --- |
| `ocr_contract_image(image_bytes, *, client=None)` | `(bytes) -> str` | 이미지 → 텍스트. 내부에서 `qwen_vision_generate()`로 Qwen2-VL을 호출 (Ollama 미사용). `client` 인자는 시그니처 호환용 |
| `describe_image(image_bytes, *, client=None)` | `(bytes) -> str` | 이미지를 한 줄로 묘사. 그라운딩 검증용 (`ocr_image.py --verify`) |
| `analyze_contract(contract_text, *, client=None)` | `(str) -> dict` | 텍스트에서 독소조항 분석 (커스텀 모델 있으면 사용, 없으면 `TEXT_MODEL` 폴백) |

### 프롬프트 상수

| 변수명 | 의미 |
| --- | --- |
| `CONTRACT_OCR_PROMPT` | 계약서 이미지 OCR용 프롬프트 (한글, 간결한 transcribe 지시). 2B-Instruct 모델 거부 응답 회피를 위해 단순화됨 |
| `IMAGE_DESCRIPTION_PROMPT` | 이미지 한 줄 묘사 프롬프트 (`describe_image`에서 사용) |
| `FALLBACK_ANALYSIS_PROMPT` | 커스텀 모델 없을 때 `TEXT_MODEL`에 주입할 전체 프롬프트 (시스템 프롬프트 + 계약서 본문) |
| `_VALID_RISK_LEVELS` | `{"높음", "중간", "낮음"}` — 정규화 기준 집합 |

### `analyze_contract` 반환 dict

| 키 | 타입 | 의미 |
| --- | --- | --- |
| `risk_level` | `string` | 전체 위험도 (정규화됨) |
| `summary` | `string` | 한 줄 요약 |
| `toxic_clauses` | `array<dict>` | `clause`, `reason`, `severity`, `recommendation` 4개 키 정규화된 객체 |
| `error` | `string` (옵션) | LLM 호출/파싱 실패 시에만 포함 |

### 정규화 매핑 (LLM이 다른 키·라벨을 써도 통일)

| 입력 키/값 (LLM 출력) | 정규화 후 |
| --- | --- |
| `조항` | `clause` |
| `impact`, `이유`, `근거` | `reason` |
| `위험도`, `risk` | `severity` |
| `advice`, `권장`, `조치` | `recommendation` |
| `독소조항` | `toxic_clauses` |
| `요약` | `summary` |
| `빨강` / `노랑` / `초록` | `높음` / `중간` / `낮음` |
| `high` / `medium` / `low` | `높음` / `중간` / `낮음` |

---

## 6-A. OCR 백엔드 (Qwen2-VL)
[server/llm/qwen_ocr.py](server/llm/qwen_ocr.py)

HuggingFace `Qwen2VLForConditionalGeneration`을 로컬 PyTorch에서 직접 실행. 모델·프로세서는 모듈 단위 싱글톤으로 한 번만 로드.

### 모듈 상수·내부 변수

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `QWEN_OCR_MODEL` | `string` | 사용할 HF 모델 ID (환경변수로 오버라이드) |
| `QWEN_OCR_DEVICE` | `string` | 사용할 디바이스 (환경변수로 오버라이드) |
| `QWEN_OCR_DTYPE` | `string` | 가중치 dtype 문자열 |
| `_model` | `Qwen2VLForConditionalGeneration \| None` | 로드된 모델 인스턴스 (싱글톤) |
| `_processor` | `AutoProcessor \| None` | 토크나이저+이미지 프로세서 (싱글톤) |
| `_load_lock` | `threading.Lock` | 동시 로딩 방지 락 |

### 공개 함수

| 함수 | 시그니처 | 의미 |
| --- | --- | --- |
| `qwen_vision_generate(prompt, image_bytes, *, max_new_tokens=2048, do_sample=False, temperature=0.0)` | `(str, bytes, ...) -> str` | 이미지 + 프롬프트로 Qwen2-VL을 호출. 동기 추론을 `asyncio.to_thread`로 감싸 async로 반환 |

### 내부 헬퍼

| 함수 | 의미 |
| --- | --- |
| `_resolve_device()` | `QWEN_OCR_DEVICE` → 환경 자동 감지 순으로 디바이스 결정 |
| `_resolve_dtype(device)` | `QWEN_OCR_DTYPE` → cuda면 float16, cpu면 float32 |
| `_load()` | 모델·프로세서를 한 번만 로드(`from_pretrained`)하고 캐싱 |
| `_run_generate(image_bytes, prompt, ...)` | 동기 추론 본체 (PIL 변환 → chat template → `model.generate` → decode) |
| `_llm_debug()` | `LLM_DEBUG=1` 여부 확인 |

---

## 6-B. OCR CLI 변수
[server/ocr_image.py](server/ocr_image.py)

이미지 한 장을 `data/contract_exN.txt`로 저장하는 단독 스크립트.

### 모듈 상수

| 변수명 | 값 | 의미 |
| --- | --- | --- |
| `REPO_ROOT` | `Path(__file__).resolve().parent.parent` | 저장소 루트 |
| `DEFAULT_DATA_DIR` | `REPO_ROOT / "data"` | 결과 저장 폴더 기본값 |
| `DEFAULT_IMAGE_DIR` | `REPO_ROOT / "data" / "image"` | 입력 이미지 폴더 기본값 |
| `DEFAULT_PREFIX` | `"contract"` | 출력 파일명 접두사 (→ `contract_exN.txt`) |
| `CHARS_PER_KB_THRESHOLD` | `180` | 이미지 KB 대비 글자 수가 이 값을 넘으면 환각 경고 |
| `MIN_UNIQUE_LINE_RATIO` | `0.5` | 고유 줄 비율이 이 값 미만이면 줄 반복 환각 경고 |
| `MAX_REPEATED_PREFIX` | `15` | 같은 라벨이 이 횟수 이상 반복되면 양식 cycling 경고 |
| `LINE_PREFIX_RE` | `regex` | 줄 시작 라벨(예: `Name:`)을 잡는 정규식 |
| `FILLER_PHRASES` | `("the quick brown fox", "lorem ipsum", "sample document")` | 학습 데이터 filler 감지 키워드 |

### 함수

| 함수 | 시그니처 | 의미 |
| --- | --- | --- |
| `resolve_image_path(raw, image_dir)` | `(Path, Path) -> Path` | 절대경로/존재 경로는 그대로, 아니면 `image_dir` 기준으로 해석 |
| `next_index(data_dir, prefix)` | `(Path, str) -> int` | **빈 번호 채우기** 방식으로 사용되지 않은 가장 작은 N(>=1) 반환 |
| `analyze_ocr_output(text, image_bytes)` | `(str, int) -> (dict, list[str])` | (stats, warnings) 반환. 4가지 환각 신호 검사 |
| `main()` | async | CLI 진입점 |

### CLI 인자

| 인자 | 기본값 | 의미 |
| --- | --- | --- |
| `image` (필수, positional) | — | OCR할 이미지 경로 (파일명만 주면 `DEFAULT_IMAGE_DIR`에서 찾음) |
| `--image-dir` | `DEFAULT_IMAGE_DIR` | 입력 이미지 폴더 |
| `--data-dir` | `DEFAULT_DATA_DIR` | 결과 저장 폴더 |
| `--prefix` | `"contract"` | 파일명 접두사 |
| `--index N` | — | 저장 번호 N 직접 지정 |
| `--overwrite` | `False` | `--index` 지정 시 동일 파일 덮어쓰기 허용 |
| `--verify` | `False` | OCR 전에 `describe_image()`로 그라운딩 확인 |
| `--debug` | `False` | `LLM_DEBUG=1` 환경변수 설정 후 실행 |

### 출력 통계 키 (`analyze_ocr_output` 반환 dict)

| 키 | 의미 |
| --- | --- |
| `chars` | 출력 총 글자 수 |
| `lines` | 비어있지 않은 줄 수 |
| `unique_lines` | 중복 제거한 고유 줄 수 |
| `chars_per_kb` | 이미지 KB 대비 글자 수 |

---

## 7. 커스텀 모델 빌더
[server/llm/modelfile.py](server/llm/modelfile.py)

### 공개 함수·상수

| 변수/함수 | 의미 |
| --- | --- |
| `TOXIC_DETECTOR_SYSTEM_PROMPT` | 독소조항 탐지 모델용 시스템 프롬프트 (한국어, JSON 스키마 강제 지시 포함) |
| `build_model_spec(...)` | `/api/create`에 보낼 구조화 payload 생성 (`from`, `system`, `parameters`) |
| `build_modelfile(...)` | Modelfile 텍스트 표현 (디버깅·저장용) |
| `load_references_from_dir(directory)` | 디렉토리 내 `.txt`/`.md`를 참조 자료로 로드 |
| `create_toxic_detector(name=..., base_model=..., references_dir=..., extra_references=..., client=...)` | Ollama에 커스텀 모델 등록 |

### `build_model_spec` 파라미터

| 변수명 | 타입 | 기본값 | 의미 |
| --- | --- | --- | --- |
| `base_model` | `string` | `TEXT_MODEL` | 베이스 모델 태그 |
| `system_prompt` | `string` | `TOXIC_DETECTOR_SYSTEM_PROMPT` | 시스템 프롬프트 |
| `references` | `Iterable[str] \| None` | `None` | 시스템 프롬프트 하단에 첨부할 참조 자료 |
| `temperature` | `float` | `0.1` | — |
| `top_p` | `float` | `0.9` | — |
| `num_ctx` | `int` | `8192` | — |
| `repeat_penalty` | `float` | `1.1` | — |

### LLM이 반드시 출력해야 하는 JSON 스키마
(`TOXIC_DETECTOR_SYSTEM_PROMPT` 내에 명시)

```json
{
  "risk_level": "높음 | 중간 | 낮음",
  "summary": "계약서 전체에 대한 한 줄 요약",
  "toxic_clauses": [
    {
      "clause": "문제 조항 원문",
      "reason": "왜 불리한지 / 근거 법령",
      "severity": "높음 | 중간 | 낮음",
      "recommendation": "세입자 조치"
    }
  ]
}
```

---

## 8. 헬스체크 `GET /health`
[server/main.py:39](server/main.py#L39)

**응답 body**

| 변수명 | 타입 | 의미 |
| --- | --- | --- |
| `status` | `string` | 항상 `"ok"` |
| `ollama` | `string` | `"connected"` / `"disconnected"` |

---

## 9. 프론트엔드 연동 체크리스트

- **인증 토큰**: `/auth/login` 응답의 `access_token`을 저장 → 이후 모든 `/contract/*`, `/auth/me` 호출에 `Authorization: Bearer <token>` 헤더 첨부.
- **분석 요청 두 가지 경로**
  - 이미지 → `POST /contract/analyze` (`multipart/form-data`, `file` 필드)
  - 텍스트 → `POST /contract/analyze-text` (`application/json`, `{"text": "..."}`)
- **위험도 라벨 통일**: 화면 분기는 한국어 `"높음" / "중간" / "낮음" / "알 수 없음"` 4종만 처리.
- **`toxic_clauses`는 항상 배열**: 빈 배열(`[]`)이면 독소조항 없음.
- **목록 vs 상세 분리**: `/contract/history`는 가벼운 메타만, 전체 결과는 `/contract/{analysis_id}`에서 가져옴.
- **에러 응답 포맷**: FastAPI 기본 `{"detail": "..."}`. 주요 상태 코드: 400(잘못된 입력), 401(인증 실패), 404(권한 없음/없음), 422(OCR 실패), 502(LLM 호출 실패).
- **CORS**: 현재 `allow_origins=["*"]`로 모든 출처 허용 ([server/main.py:29](server/main.py#L29)).
