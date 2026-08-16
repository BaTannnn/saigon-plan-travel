from app.db.postgres import pool
from app.rag.embedding_service import embed_query
from app.rag.knowledge_repository import search_candidate_chunks


def main() -> None:
    query = "Tôi thích mỹ thuật và kiến trúc"

    query_embedding = embed_query(query)

    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    results = search_candidate_chunks(
        query_embedding=query_embedding,
        limit=30,
    )

    print(f"\nQuery: {query}\n")

    for index, result in enumerate(results, start=1):
        print(
            f"{index}. "
            f"{result.place_name} | "
            f"{result.section} | "
            f"{result.similarity:.4f} | "
            f"embedding={len(result.embedding)}"
        )


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()