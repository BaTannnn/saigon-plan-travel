from app.db.postgres import pool
from app.rag.embedding_service import embed_query
from app.rag.knowledge_repository import search_candidate_chunks
from app.recommendation.mmr import select_with_mmr
from app.recommendation.models import PlaceCandidate
from app.recommendation.place_retriever import (
    select_unique_candidate_chunks,
)


def main() -> None:
    query = "Tôi thích nghệ thuật, văn hóa và không gian sáng tạo"

    query_embedding = embed_query(query)

    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    chunks = search_candidate_chunks(
        query_embedding=query_embedding,
        limit=30,
    )

    unique_chunks = select_unique_candidate_chunks(
        results=chunks,
        limit=15,
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

    embeddings = {
        chunk.place_slug: chunk.embedding
        for chunk in unique_chunks
    }

    semantic_top = candidates[:5]

    mmr_top = select_with_mmr(
        candidates=candidates,
        embeddings_by_slug=embeddings,
        limit=5,
        lambda_weight=0.7,
    )

    print(f"\nQuery: {query}")

    print("\n=== SEMANTIC TOP-K ===")

    for index, candidate in enumerate(semantic_top, start=1):
        print(
            f"{index}. "
            f"{candidate.place_name} | "
            f"{candidate.semantic_score:.4f}"
        )

    print("\n=== MMR TOP-K ===")

    for index, candidate in enumerate(mmr_top, start=1):
        print(
            f"{index}. "
            f"{candidate.place_name} | "
            f"{candidate.semantic_score:.4f}"
        )


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()