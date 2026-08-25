from fastapi import APIRouter
from typing import Annotated

from pydantic import BaseModel, Field, field_validator

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

    eligible_place_slugs: list[
        Annotated[str, Field(strict=True, min_length=1, max_length=180)]
    ]

    @field_validator("eligible_place_slugs")
    @classmethod
    def normalize_eligible_place_slugs(
        cls,
        slugs: list[str],
    ) -> list[str]:
        normalized_slugs: list[str] = []
        seen: set[str] = set()

        for slug in slugs:
            normalized_slug = slug.strip()

            if not normalized_slug:
                raise ValueError("eligible place slugs must not be blank")

            if normalized_slug not in seen:
                seen.add(normalized_slug)
                normalized_slugs.append(normalized_slug)

        return normalized_slugs


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
        eligible_place_slugs=request.eligible_place_slugs,
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
