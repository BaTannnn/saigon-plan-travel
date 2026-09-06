from app.config import get_retrieval_fetch_k
from app.knowledge.embedding_service import embed_query
from app.knowledge.retrieval_repository import search_candidate_chunks
from app.recommendation.models import PlaceCandidate
from app.recommendation.place_retriever import (
    deduplicate_best_chunk_by_place,
    rank_by_semantic_similarity,
)


def retrieve_candidate_places(
    query: str,
    top_k: int,
    fetch_k: int | None = None,
) -> list[PlaceCandidate]:
    query_embedding = embed_query(query)

    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    if top_k <= 0:
        raise ValueError("top_k must be greater than zero")

    resolved_fetch_k = get_retrieval_fetch_k(fetch_k)

    while True:
        retrieved_chunks = search_candidate_chunks(
            query_embedding=query_embedding,
            limit=resolved_fetch_k,
        )

        unique_chunks = deduplicate_best_chunk_by_place(
            retrieved_chunks,
        )

        if len(unique_chunks) >= top_k or len(retrieved_chunks) < resolved_fetch_k:
            break

        resolved_fetch_k *= 2

    ranked_chunks = rank_by_semantic_similarity(unique_chunks)
    limited_chunks = ranked_chunks[:top_k]

    candidates = [
        PlaceCandidate(
            place_slug=chunk.place_slug,
            place_name=chunk.place_name,
            matched_section=chunk.section,
            semantic_score=chunk.similarity,
        )
        for chunk in limited_chunks
    ]

    return candidates


def recommend_places(
    query: str,
    fetch_k: int | None = None,
    top_k: int = 15,
) -> list[PlaceCandidate]:
    return retrieve_candidate_places(
        query=query,
        top_k=top_k,
        fetch_k=fetch_k,
    )
