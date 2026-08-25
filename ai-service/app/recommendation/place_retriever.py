from app.knowledge.knowledge_repository import CandidateChunk


def select_unique_candidate_chunks(
    results: list[CandidateChunk],
    limit: int,
) -> list[CandidateChunk]:
    best_by_place: dict[str, CandidateChunk] = {}

    for result in results:
        current = best_by_place.get(result.place_slug)

        if current is None or result.similarity > current.similarity:
            best_by_place[result.place_slug] = result

    ranked = sorted(
        best_by_place.values(),
        key=lambda result: result.similarity,
        reverse=True,
    )

    return ranked[:limit]
