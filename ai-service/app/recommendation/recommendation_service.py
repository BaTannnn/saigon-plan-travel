from app.config import (
    get_mmr_lambda,
    get_retrieval_candidate_k,
    get_retrieval_fetch_k,
    get_retrieval_mode,
)
from app.knowledge.embedding_service import embed_query
from app.knowledge.knowledge_repository import search_candidate_chunks
from app.recommendation.mmr import select_with_mmr
from app.recommendation.models import PlaceCandidate
from app.recommendation.place_retriever import (
    deduplicate_best_chunk_by_place,
    rank_by_semantic_similarity,
)


def retrieve_candidate_places(
    query: str,
    fetch_k: int | None = None,
    candidate_k: int | None = None,
) -> tuple[list[PlaceCandidate], dict[str, list[float]]]:
    query_embedding = embed_query(query)

    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    resolved_fetch_k = get_retrieval_fetch_k(fetch_k)
    resolved_candidate_k = get_retrieval_candidate_k(candidate_k)
    target_unique_places = resolved_candidate_k

    while True:
        retrieved_chunks = search_candidate_chunks(
            query_embedding=query_embedding,
            limit=resolved_fetch_k,
        )

        unique_chunks = deduplicate_best_chunk_by_place(
            retrieved_chunks,
        )

        if (
            len(unique_chunks) >= target_unique_places
            or len(retrieved_chunks) < resolved_fetch_k
        ):
            break

        resolved_fetch_k *= 2

    ranked_chunks = rank_by_semantic_similarity(unique_chunks)
    limited_chunks = ranked_chunks[:resolved_candidate_k]

    candidates = [
        PlaceCandidate(
            place_slug=chunk.place_slug,
            place_name=chunk.place_name,
            matched_section=chunk.section,
            semantic_score=chunk.similarity,
        )
        for chunk in limited_chunks
    ]

    embeddings_by_slug = {
        chunk.place_slug: chunk.embedding
        for chunk in limited_chunks
    }

    return candidates, embeddings_by_slug


def recommend_places(
    query: str,
    fetch_k: int | None = None,
    candidate_k: int | None = None,
    top_k: int = 15,
    retrieval_mode: str | None = None,
    lambda_weight: float | None = None,
) -> list[PlaceCandidate]:
    mode = get_retrieval_mode(retrieval_mode)
    if mode == "semantic":
        pool_size = top_k
    else:
        pool_size = max(
            get_retrieval_candidate_k(candidate_k),
            top_k,
        )
    candidates, embeddings_by_slug = retrieve_candidate_places(
        query=query,
        fetch_k=fetch_k,
        candidate_k=pool_size,
    )

    if mode == "semantic":
        return candidates[:top_k]

    return select_with_mmr(
        candidates=candidates,
        embeddings_by_slug=embeddings_by_slug,
        limit=top_k,
        lambda_weight=get_mmr_lambda(lambda_weight),
    )
