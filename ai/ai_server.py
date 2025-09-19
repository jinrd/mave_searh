from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# 미리 학습된 한국어 모델 로드
# 처음 실행 시 모델 파일을 다운로드하므로 시간이 걸릴 수 있다
model = SentenceTransformer('jhgan/ko-sroberta-multitask')

app = FastAPI()

class TextRequest(BaseModel):
    text: str

@app.post("/embed")
def get_embedding(request: TextRequest):
    """
    입력된 텍스트를 벡터 임베딩으로 변환하여 반환한다.
    """
    embedding = model.encode(request.text)

    # JSON 으로 보내기 위해 리스트로 변환
    return {"embedding": embedding.tolist()}

# 이 파일을 실행하기 위해서 source venv/bin/activate 실행하여 가상화 환경 만든 후에
# uvicorn ai.ai_server:app --reload 명령어로 Unicorn 고성능 서버 실행
# 서버 설치는
# python -m pip install uvicorn fastapi "sentence-transformers[ko]"