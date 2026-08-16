from app.rag.embedding_service import embed_query
from app.rag.knowledge_repository import search_candidate_chunks
from app.recommendation.mmr import select_with_mmr
from app.recommendation.models import PlaceCandidate
from app.recommendation.place_retriever import (
    select_unique_candidate_chunks,
)


def recommend_places(
    query: str,
    fetch_k: int = 50,
    candidate_k: int = 30,
    top_k: int = 15,
    lambda_weight: float = 0.7,
) -> list[PlaceCandidate]:
    query_embedding = embed_query(query)

    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    retrieved_chunks = search_candidate_chunks(
        query_embedding=query_embedding,
        limit=fetch_k,
    )

    unique_chunks = select_unique_candidate_chunks(
        results=retrieved_chunks,
        limit=candidate_k,
    )

    candidates = [
        PlaceCandidate(
            place_slug=chunk.place_slug,
            place_name=chunk.place_name,
            matched_section=chunk.section,
            semantic_score=chunk.similarity,
        )
        for chunk in unique_chunks
    ]

    embeddings_by_slug = {
        chunk.place_slug: chunk.embedding
        for chunk in unique_chunks
    }

    return select_with_mmr(
        candidates=candidates,
        embeddings_by_slug=embeddings_by_slug,
        limit=top_k,
        lambda_weight=lambda_weight,
    )