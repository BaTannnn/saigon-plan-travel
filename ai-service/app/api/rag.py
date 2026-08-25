from fastapi import APIRouter
from pydantic import BaseModel, Field, field_validator

from app.rag.itinerary_explanation_service import generate_itinerary_reasons
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


class ItineraryReasonsRequest(BaseModel):
    preference: str = Field(
        min_length=3,
        max_length=1000,
    )
    place_slugs: list[str] = Field(
        min_length=1,
        max_length=30,
    )

    @field_validator("place_slugs")
    @classmethod
    def normalize_place_slugs(cls, place_slugs: list[str]) -> list[str]:
        normalized = list(
            dict.fromkeys(
                slug.strip()
                for slug in place_slugs
                if slug.strip()
            )
        )
        if not normalized:
            raise ValueError("At least one non-blank place slug is required")
        return normalized


class ItineraryReasonResponse(BaseModel):
    place_slug: str
    reason: str | None


class ItineraryReasonsResponse(BaseModel):
    reasons: list[ItineraryReasonResponse]


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


@router.post(
    "/itinerary-reasons",
    response_model=ItineraryReasonsResponse,
)
def explain_itinerary(
    request: ItineraryReasonsRequest,
) -> ItineraryReasonsResponse:
    reasons = generate_itinerary_reasons(
        preference=request.preference.strip(),
        place_slugs=request.place_slugs,
    )

    return ItineraryReasonsResponse(
        reasons=[
            ItineraryReasonResponse(
                place_slug=item.place_slug,
                reason=item.reason,
            )
            for item in reasons
        ]
    )
