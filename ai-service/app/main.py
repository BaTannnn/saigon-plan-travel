from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.rag import router as rag_router
from app.db.postgres import pool


@asynccontextmanager
async def lifespan(app: FastAPI):
    pool.open()
    pool.wait()

    yield

    pool.close()


app = FastAPI(
    title="SaigonPlanTravel AI Service",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(rag_router)