from app.knowledge.knowledge_repository import CandidateChunk


def deduplicate_best_chunk_by_place(
    chunks: list[CandidateChunk],
) -> list[CandidateChunk]:
    best_by_place: dict[str, CandidateChunk] = {}

    for chunk in chunks:
        current = best_by_place.get(chunk.place_slug)

        if current is None or chunk.similarity > current.similarity:
            best_by_place[chunk.place_slug] = chunk

    return list(best_by_place.values())


def rank_by_semantic_similarity(
    chunks: list[CandidateChunk],
) -> list[CandidateChunk]:
    return sorted(
        chunks,
        key=lambda chunk: chunk.similarity,
        reverse=True,
    )
