from dotenv import load_dotenv

from app.knowledge.embedding_service import (
    EMBEDDING_DIMENSION,
    embed_document,
    embed_query,
)

load_dotenv()


def main() -> None:
    document = """
    Dinh Độc Lập là một di tích lịch sử và
    công trình kiến trúc tiêu biểu tại
    Thành phố Hồ Chí Minh.
    """

    query = "Tôi muốn tìm hiểu địa điểm lịch sử"

    document_embedding = embed_document(
        content=document,
        title="Dinh Độc Lập - OVERVIEW",
    )

    query_embedding = embed_query(query)

    print(f"Document vector dimension: {len(document_embedding)}")

    print(f"Query vector dimension: {len(query_embedding)}")

    print(
        "Document first 5 values:",
        document_embedding[:5],
    )

    print(
        "Query first 5 values:",
        query_embedding[:5],
    )

    assert len(document_embedding) == EMBEDDING_DIMENSION
    assert len(query_embedding) == EMBEDDING_DIMENSION


if __name__ == "__main__":
    main()
