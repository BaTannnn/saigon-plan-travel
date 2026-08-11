from fastapi import FastAPI

from app.api.rag import router as rag_router


app = FastAPI(
    title="SaigonPlanTravel AI Service",
    version="0.1.0",
)

app.include_router(rag_router)

@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok",
    }