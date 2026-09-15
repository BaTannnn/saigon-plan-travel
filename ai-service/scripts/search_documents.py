from app.db.postgres import pool
from app.knowledge.embedding_service import embed_query
from app.knowledge.retrieval_repository import search_similar_document_chunks


def main() -> None:
    query = input("Query: ").strip()
    if not query:
        return

    embedding = embed_query(query)
    if embedding is None:
        raise RuntimeError("Could not create query embedding")

    results = search_similar_document_chunks(
        query_embedding=embedding,
        limit=5,
    )

    print()
    for index, result in enumerate(results, start=1):
        print(
            f"{index}. {result.document_title} — page {result.page_number} "
            f"[chunk {result.chunk_index}]"
        )
        print(f"   similarity={result.similarity:.4f}")
        print(f"   source={result.source_uri}")
        print(f"   {result.content[:160]}...")
        print()


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()
