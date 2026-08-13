import math

from app.recommendation.models import PlaceCandidate


def cosine_similarity(
    left: list[float],
    right: list[float],
) -> float:
    if len(left) != len(right):
        raise ValueError("Embeddings must have the same dimension")

    dot_product = sum(a * b for a, b in zip(left, right))
    left_norm = math.sqrt(sum(value * value for value in left))
    right_norm = math.sqrt(sum(value * value for value in right))

    if left_norm == 0 or right_norm == 0:
        return 0.0

    return dot_product / (left_norm * right_norm)


def select_with_mmr(
    candidates: list[PlaceCandidate],
    embeddings_by_slug: dict[str, list[float]],
    limit: int,
    lambda_weight: float = 0.7,
) -> list[PlaceCandidate]:
    if not 0.0 <= lambda_weight <= 1.0:
        raise ValueError("lambda_weight must be between 0 and 1")

    if limit <= 0 or not candidates:
        return []

    for candidate in candidates:
        if candidate.place_slug not in embeddings_by_slug:
            raise ValueError(
                f"Missing embedding for place: {candidate.place_slug}"
            )

    remaining = candidates.copy()
    selected: list[PlaceCandidate] = []

    first = max(
        remaining,
        key=lambda candidate: candidate.semantic_score,
    )

    selected.append(first)
    remaining.remove(first)

    while remaining and len(selected) < limit:
        best_candidate = max(
            remaining,
            key=lambda candidate: _mmr_score(
                candidate=candidate,
                selected=selected,
                embeddings_by_slug=embeddings_by_slug,
                lambda_weight=lambda_weight,
            ),
        )

        selected.append(best_candidate)
        remaining.remove(best_candidate)

    return selected


def _mmr_score(
    candidate: PlaceCandidate,
    selected: list[PlaceCandidate],
    embeddings_by_slug: dict[str, list[float]],
    lambda_weight: float,
) -> float:
    candidate_embedding = embeddings_by_slug[candidate.place_slug]

    max_similarity_to_selected = max(
        cosine_similarity(
            candidate_embedding,
            embeddings_by_slug[selected_candidate.place_slug],
        )
        for selected_candidate in selected
    )

    return (
        lambda_weight * candidate.semantic_score
        - (1.0 - lambda_weight) * max_similarity_to_selected
    )