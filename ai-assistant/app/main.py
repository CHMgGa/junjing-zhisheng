import uvicorn
from fastapi import FastAPI

from app.experts import chat, vision_diagnosis
from app.knowledge_base import knowledge_base
from app.schemas import AiResult, ChatRequest, IndexResult, SearchRequest, VisionDiagnosisRequest

app = FastAPI(title="Morel AI Service", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "service": "morel-ai-service"}


@app.post("/api/index/rebuild", response_model=IndexResult)
def rebuild_index() -> IndexResult:
    document_count, chunk_count = knowledge_base.rebuild()
    return IndexResult(document_count=document_count, chunk_count=chunk_count)


@app.post("/api/search")
def search(request: SearchRequest) -> dict:
    return {"references": knowledge_base.search(request.query, request.top_k)}


@app.post("/api/chat", response_model=AiResult)
async def chat_endpoint(request: ChatRequest) -> AiResult:
    return await chat(request)


@app.post("/api/vision-diagnosis", response_model=AiResult)
async def vision_endpoint(request: VisionDiagnosisRequest) -> AiResult:
    return await vision_diagnosis(request)


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=18080, reload=False)
