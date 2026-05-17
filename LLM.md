# LLM 모듈 구조

전월세 계약서 이미지를 OCR → 독소조항 분석으로 처리하는 LLM 관련 코드.
- **OCR**: HuggingFace transformers로 로컬 PyTorch에서 Qwen2-VL을 직접 실행 (Ollama 미사용).
- **독소조항 분석**: Ollama로 텍스트 모델(`TEXT_MODEL`) 호출, 커스텀 모델(`dreamlog-toxic-detector`)을 학습 자료와 함께 등록 가능.

## 디렉토리 구조

```
dreamlog/
├── data/
│   ├── contract_ex1.txt           # 분석 검증용 예시 계약서 (전세 표준계약서 + 독소조항 혼합)
│   ├── contract_ex2.txt …         # OCR 출력물 (ocr_image.py가 빈 번호 채우기로 자동 할당)
│   ├── contract_rule.xlsx         # 독소조항 학습 자료 (라벨링된 조항 · 법 조문)
│   └── image/
│       └── ocr_test_sample.png    # OCR 입력용 샘플 이미지 폴더
└── server/
    ├── main.py                    # FastAPI 진입점 — lifespan에서 Ollama health 체크
    ├── .env.example               # Ollama 모델·서버 URL 환경변수 템플릿
    ├── requirements.txt           # openpyxl + torch/transformers/accelerate (OCR용)
    ├── llm_test.py                # 학습-검증 CLI: rule xlsx로 모델 재등록 후 ex1.txt 분석
    ├── ocr_image.py               # OCR 전용 CLI: 이미지 → data/contract_exN.txt
    ├── contract/
    │   └── router.py              # /contract/analyze — OCR + analyze_contract 호출
    └── llm/
        ├── __init__.py            # 모듈 진입점 (외부에 공개할 심볼 묶음)
        ├── config.py              # LLMConfig 데이터클래스 + 용도별 프리셋
        ├── client.py              # Ollama HTTP API 비동기 래퍼 (분석용)
        ├── qwen_ocr.py            # HuggingFace Qwen2-VL 로컬 비전 추론 백엔드 (OCR용)
        ├── tasks.py               # 고수준 태스크 (OCR / 이미지 묘사 / 계약서 분석)
        └── modelfile.py           # 커스텀 모델 빌더 (시스템 프롬프트 + 참조 자료 주입)
```

## `server/llm/` — 새로 만든 LLM 모듈

### `__init__.py`
모듈 진입점. 외부(`server/main.py`, `server/contract/router.py`, `server/llm_test.py`, `server/ocr_image.py`)에서 사용할 함수·상수를 한 번에 export한다.

```python
from llm import (
    analyze_contract, ocr_contract_image, describe_image,
    get_client, create_toxic_detector,
)
```

### `config.py`
- `LLMConfig` 데이터클래스: Ollama `/api/generate`·`/api/chat` 호출에 들어가는 모든 파라미터(모델명·temperature·top_p·num_ctx·format 등)를 한 객체로 묶음. `to_payload()` / `to_options()` / `merged()` 헬퍼 제공.
- 용도별 프리셋:
  - `get_vision_config()` — Ollama 비전 모델용. 현재 OCR은 `qwen_ocr.py`로 우회하므로 미사용 (향후 Ollama llava로 폴백 시 사용).
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

### `qwen_ocr.py`
HuggingFace `Qwen2VLForConditionalGeneration` 기반 로컬 비전 추론 백엔드. `transformers` + `torch`로 모델을 한 번만 로드해 모듈 단위 싱글톤으로 재사용한다.

- **`qwen_vision_generate(prompt, image_bytes, *, max_new_tokens, do_sample, temperature)`** — 이미지 + 프롬프트로 Qwen2-VL을 호출. 동기 추론을 `asyncio.to_thread`로 감싸 async 인터페이스로 노출.
- 환경변수: `QWEN_OCR_MODEL`(기본 `Qwen/Qwen2-VL-2B-Instruct`), `QWEN_OCR_DEVICE`(자동: cuda/cpu), `QWEN_OCR_DTYPE`(자동: float16/float32).
- `LLM_DEBUG=1`이면 로딩 디바이스·dtype, 이미지 바이트 수, 출력 글자 수 등을 stderr에 남김.

### `tasks.py`
고수준 태스크 세 개:

- **`ocr_contract_image(image_bytes)`** — 계약서 이미지 → 텍스트. `qwen_vision_generate`로 Qwen2-VL을 호출하며 `CONTRACT_OCR_PROMPT`(간결한 transcribe 지시)를 함께 보낸다.
- **`describe_image(image_bytes)`** — 이미지를 한 줄로 묘사. `IMAGE_DESCRIPTION_PROMPT`로 그라운딩 검증용 (`ocr_image.py --verify`에서 사용).
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

## `server/ocr_image.py` — OCR 전용 CLI

이미지 한 장을 `data/contract_exN.txt`로 저장하는 단독 스크립트. 분석 단계 없이 OCR만 검증할 때 사용.

흐름:
1. 입력 경로 해석 — 파일명만 주면 `data/image/`에서 찾고, 절대경로는 그대로 사용.
2. `next_index(data_dir, prefix)`로 **비어있는 가장 작은 N**을 찾아 파일명 결정 (max+1이 아닌 빈 번호 채우기 방식).
3. `ocr_contract_image()`로 Qwen2-VL 호출 → 텍스트 반환.
4. `analyze_ocr_output()`이 출력 길이 대비 이미지 크기, 줄 중복률, 동일 라벨 반복 등 환각 신호를 검사해 경고.
5. 결과를 `data/contract_exN.txt`에 저장.

옵션:
```
python3 ocr_image.py sample.png                  # data/image/sample.png 사용
python3 ocr_image.py sample.png --verify         # OCR 전에 describe_image로 그라운딩 확인
python3 ocr_image.py sample.png --debug          # qwen_ocr 디버그 로그(LLM_DEBUG=1)
python3 ocr_image.py sample.png --index 5        # contract_ex5.txt로 강제 지정
python3 ocr_image.py sample.png --index 5 --overwrite
python3 ocr_image.py sample.png --prefix invoice # invoice_ex1.txt 형식
```

## `server/` 쪽 통합 지점

- **`main.py`** — lifespan 훅에서 `get_client().health()`로 Ollama 연결 상태를 체크하고 로그 출력.
- **`contract/router.py`** — `POST /contract/analyze`: 업로드된 이미지를 `ocr_contract_image()`로 텍스트화한 뒤 `analyze_contract()`에 넘기고, 결과를 `Analysis` 테이블에 저장. (기존 `contract/analyzer.py`는 제거됨 — 로직이 `llm/tasks.py`로 이동)
- **`.env.example`** — `OLLAMA_BASE_URL`, `OLLAMA_TIMEOUT`, `VISION_MODEL`, `TEXT_MODEL`, `TOXIC_DETECTOR_MODEL` 키 추가. (OCR 전환 시 `QWEN_OCR_MODEL` 등도 함께 관리 권장)
- **`requirements.txt`** — xlsx 파싱용 `openpyxl==3.1.5`, OCR용 `torch>=2.4` / `transformers>=4.45` / `accelerate>=0.34`, 이미지 처리용 `Pillow==11.0.0` 추가.

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

## OCR 테스트 환경 설정 (팀원 공유용)

OCR은 **Ollama가 아니라 HuggingFace transformers + 로컬 PyTorch**로 Qwen2-VL을 직접 호출한다 ([server/llm/qwen_ocr.py](server/llm/qwen_ocr.py)). 따라서 `ollama serve`는 필요 없고, HuggingFace 캐시에 모델만 있으면 된다.

### 1) 필수 패키지

`server/requirements.txt`에 작성돼있음

```bash
# GPU(권장) — CUDA 12.x 머신
pip install "torch>=2.4" "transformers>=4.45" Pillow accelerate

# CPU만 — 추론 매우 느림(분 단위), 테스트용으로만
pip install torch transformers Pillow
```

> 검증된 조합: `torch==2.10.0+cu128`, `transformers==4.57.6`, CUDA 12.8.
> `Qwen2VLForConditionalGeneration` 클래스를 쓰려면 `transformers>=4.45` 필요.

### 2) GPU / 디스크 요구사항

| 항목 | 권장 | 비고 |
| --- | --- | --- |
| GPU VRAM | ≥ 8GB (Qwen2-VL-2B, float16 기준) | 7B 모델은 ≥ 20GB |
| HF 캐시 디스크 | ≥ 8GB 여유 | 첫 실행 시 모델 자동 다운로드 |
| CUDA | 12.x (또는 본인 torch와 호환되는 버전) | `python3 -c "import torch; print(torch.cuda.is_available())"`로 확인 |

HuggingFace 캐시 위치는 기본 `~/.cache/huggingface/`. 다른 경로를 쓰려면 `HF_HOME` 환경변수 설정.

### 3) 환경 변수 (선택)

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `QWEN_OCR_MODEL` | `Qwen/Qwen2-VL-2B-Instruct` | HF 모델 ID. 결과 품질이 낮으면 `Qwen/Qwen2-VL-7B-Instruct`로 교체 |
| `QWEN_OCR_DEVICE` | (자동: cuda 가능하면 cuda, 아니면 cpu) | `cuda` / `cuda:0` / `cpu` |
| `QWEN_OCR_DTYPE` | (자동: cuda면 float16, cpu면 float32) | `float16` / `bfloat16` / `float32` |
| `LLM_DEBUG` | `0` | `1`로 두면 로딩·생성 메타데이터를 stderr로 출력 |

### 4) 테스트 이미지 준비

`data/image/` 폴더에 OCR할 이미지를 넣는다. 폴더가 없으면 만든다.

```bash
mkdir -p data/image
# 예: 본인 계약서 사진을 data/image/sample.png로 복사
```

`data/image/`는 `.gitignore`되어 있지 않지만, 개인 계약서가 들어갈 수 있으므로 **커밋 전에 확인**할 것.

### 5) 실행

```bash
cd server
python3 ocr_image.py sample.png         # data/image/sample.png 사용
python3 ocr_image.py /abs/path/img.jpg  # 절대 경로도 가능

# 결과 → data/contract_exN.txt (N은 비어있는 가장 작은 번호로 자동 할당)
```

유용한 옵션:

```bash
python3 ocr_image.py sample.png --verify  # OCR 전에 이미지 묘사로 그라운딩 확인
python3 ocr_image.py sample.png --debug   # 로딩·토큰 수 등 디버그 로그
python3 ocr_image.py sample.png --index 5 # contract_ex5.txt로 강제 지정
```

### 6) 첫 실행 시 발생하는 일

1. `Qwen/Qwen2-VL-2B-Instruct` 모델·프로세서를 HF Hub에서 다운로드 (≈ 4GB, 한 번만)
2. 모델을 GPU/CPU에 로드 (이후 호출은 모듈 단위로 캐싱)
3. 이미지 + 프롬프트로 추론 → `data/contract_exN.txt` 저장
4. 출력 끝에 환각 신호 감지 통계(`chars/KB`, 줄 중복률 등)도 함께 표시

### 7) 자주 만나는 문제

| 증상 | 원인 / 해결 |
| --- | --- |
| `CUDA out of memory` | 7B 모델은 VRAM 부족 가능 → 2B로 다운그레이드, 또는 `QWEN_OCR_DTYPE=bfloat16`/`float16`으로 |
| 다운로드가 멈춤 | 네트워크/방화벽 문제. `HF_HUB_OFFLINE=0` 확인, 또는 미리 `huggingface-cli download Qwen/Qwen2-VL-2B-Instruct`로 받아둠 |
| "저는 OCR을 할 수 없습니다…" 류 거부 응답 | 프롬프트가 모델 거부 패턴을 트리거. `server/llm/tasks.py`의 `CONTRACT_OCR_PROMPT`가 너무 장황하거나 부정문 위주면 발생 — 짧고 직접적인 명령으로 유지 |
| 같은 줄이 반복되는 환각 | 모델 한계. 더 큰 모델(`QWEN_OCR_MODEL=Qwen/Qwen2-VL-7B-Instruct`)로 교체하거나 이미지 품질 개선 |
| `Qwen2VLForConditionalGeneration` import 에러 | `transformers` 버전이 낮음 → `pip install -U transformers` |

> 참고: OCR만 테스트할 때는 `ollama serve`가 **필요 없다**. Ollama는 독소조항 분석(`analyze_contract`) 단계에서만 사용된다.
