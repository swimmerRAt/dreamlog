# 터미널 1 — 서버 기동 (Ollama가 떠 있어야 함)
cd server
uvicorn main:app --reload --port 8000

# 터미널 2 — 테스트
cd server
python3 server_test.py
# 결과 저장하고 싶으면
python3 server_test.py --save-json out.json