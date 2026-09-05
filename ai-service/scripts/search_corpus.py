from app.knowledge.embedding_service import embed_query
from app.knowledge.retrieval_repository import (
    search_similar_chunks,
)


def main() -> None:
    query = input("Query: ").strip()

    if not query:
        return

    embedding = embed_query(query)

    results = search_similar_chunks(
        query_embedding=embedding,
        limit=5,
    )

    print()

    for index, result in enumerate(
        results,
        start=1,
    ):
        print(f"{index}. {result.place_name} [{result.section}]")

        print(f"   similarity={result.similarity:.4f}")

        print(f"   {result.content[:160]}...")

        print()


if __name__ == "__main__":
    main()
