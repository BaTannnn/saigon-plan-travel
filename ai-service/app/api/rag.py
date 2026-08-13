from pydantic import BaseModel, Field
from fastapi import APIRouter
from app.rag.rag_service import answer_question


router = APIRouter(
    prefix="/api/v1/rag",
    tags=["RAG"],
)


class RagAskRequest(BaseModel):
    question: str = Field(
        min_length=3,
        max_length=1000,
    )

    top_k: int = Field(
        default=5,
        ge=1,
        le=10,
    )


class RagSourceResponse(BaseModel):
    place_slug: str
    place_name: str
    section: str
    source_label: str
    source_uri: str
    similarity: float


class RagAskResponse(BaseModel):
    answer: str
    sources: list[RagSourceResponse]


@router.post(
    "/ask",
    response_model=RagAskResponse,
)
def ask_rag(
    request: RagAskRequest,
) -> RagAskResponse:
    question = request.question.strip()
    result = answer_question(
        question=question,
        top_k=request.top_k,
    )

    sources = [
        RagSourceResponse(
            place_slug=source.place_slug,
            place_name=source.place_name,
            section=source.section,
            source_label=source.source_label,
            source_uri=source.source_uri,
            similarity=source.similarity,
        )
        for source in result.sources
    ]

    return RagAskResponse(
        answer=result.answer,
        sources=sources,
    )