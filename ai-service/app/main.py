import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.rag import router as rag_router
from app.api.recommendation import router as recommendation_router
from app.config import get_retrieval_mode
from app.db.postgres import pool


logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(
        "Recommendation retrieval mode: %s",
        get_retrieval_mode(),
    )
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
app.include_router(recommendation_router)
