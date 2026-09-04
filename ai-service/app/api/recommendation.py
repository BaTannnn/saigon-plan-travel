from fastapi import APIRouter

from pydantic import BaseModel, Field

from app.recommendation.recommendation_service import recommend_places


router = APIRouter(
    prefix="/api/v1/recommendations",
    tags=["Recommendation"],
)


class PlaceRecommendationRequest(BaseModel):
    query: str = Field(
        min_length=3,
        max_length=1000,
    )

    top_k: int = Field(
        default=15,
        ge=1,
        le=30,
    )

class PlaceCandidateResponse(BaseModel):
    place_slug: str
    place_name: str
    matched_section: str
    semantic_score: float


class PlaceRecommendationResponse(BaseModel):
    candidates: list[PlaceCandidateResponse]


@router.post(
    "/places",
    response_model=PlaceRecommendationResponse,
)
def recommend_place_candidates(
    request: PlaceRecommendationRequest,
) -> PlaceRecommendationResponse:
    candidates = recommend_places(
        query=request.query.strip(),
        top_k=request.top_k,
    )

    return PlaceRecommendationResponse(
        candidates=[
            PlaceCandidateResponse(
                place_slug=candidate.place_slug,
                place_name=candidate.place_name,
                matched_section=candidate.matched_section,
                semantic_score=candidate.semantic_score,
            )
            for candidate in candidates
        ]
    )
