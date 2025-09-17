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