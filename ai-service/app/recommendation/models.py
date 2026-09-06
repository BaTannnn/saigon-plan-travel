from dataclasses import dataclass


@dataclass(frozen=True)
class PlaceCandidate:
    place_slug: str
    place_name: str
    matched_section: str
    semantic_score: float
