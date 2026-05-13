# 서버 구조

```
server/
├── main.py              # FastAPI 앱 진입점
├── database.py          # SQLite DB 모델 (User, Analysis)
├── requirements.txt
├── .env                 # 환경변수 (SECRET_KEY, 모델명 등)
├── auth/
│   ├── router.py        # POST /auth/register, /auth/login, GET /auth/me
│   └── utils.py         # JWT 발급/검증, 비밀번호 해싱
└── contract/
    ├── router.py        # POST /contract/analyze, GET /contract/history, /contract/{id}
    └── analyzer.py      # Ollama 연동 (OCR → 독소조항 분석)
```

# 실행 방법

```bash
# 1. Ollama 설치 후 모델 준비
ollama pull llava        # 이미지 OCR용 (vision 모델)
ollama pull llama3.2     # 텍스트 분석용
ollama serve             # Ollama 서버 시작

# 2. 가상환경 & 의존성 설치
cd server
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 3. 서버 실행
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

# API 흐름

| 엔드포인트 | 설명 |
|---|---|
| `POST /auth/register` | 회원가입 |
| `POST /auth/login` | 로그인 → JWT 토큰 반환 |
| `POST /contract/analyze` | 계약서 이미지 업로드 → OCR + 독소조항 분석 |
| `GET /contract/history` | 내 분석 기록 목록 |
| `GET /contract/{id}` | 특정 분석 결과 상세 조회 |
| `GET /health` | 서버/Ollama 상태 확인 |

Android 앱에서 에뮬레이터로 접속할 때는 `http://10.0.2.2:8000`을 서버 주소로 사용하면 됩니다.
