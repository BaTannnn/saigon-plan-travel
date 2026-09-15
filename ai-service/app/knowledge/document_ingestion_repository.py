from dataclasses import dataclass

from pgvector import Vector

from app.db.postgres import get_connection
from app.knowledge.pdf_corpus import PreparedPdfDocument


@dataclass(frozen=True)
class StoredDocumentState:
    document_id: int
    content_hash: str


def find_stored_document_state(*, source_uri: str) -> StoredDocumentState | None:
    query = """
        SELECT id, content_hash
        FROM knowledge_documents
        WHERE source_uri = %s
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(query, (source_uri,))
            row = cursor.fetchone()

    if row is None:
        return None

    return StoredDocumentState(
        document_id=row[0],
        content_hash=row[1],
    )


def replace_document_chunks(
    *,
    document: PreparedPdfDocument,
    embeddings: list[list[float]],
) -> int:
    if len(document.chunks) != len(embeddings):
        raise ValueError("Each document chunk must have exactly one embedding")

    upsert_document_query = """
        INSERT INTO knowledge_documents (
            title,
            source_label,
            source_uri,
            content_hash
        )
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (source_uri)
        DO UPDATE SET
            title = EXCLUDED.title,
            source_label = EXCLUDED.source_label,
            content_hash = EXCLUDED.content_hash,
            updated_at = CURRENT_TIMESTAMP
        RETURNING id
    """
    delete_chunks_query = """
        DELETE FROM document_knowledge_chunks
        WHERE document_id = %s
    """
    insert_chunk_query = """
        INSERT INTO document_knowledge_chunks (
            document_id,
            chunk_index,
            page_number,
            content,
            content_hash,
            embedding
        )
        VALUES (%s, %s, %s, %s, %s, %s)
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                upsert_document_query,
                (
                    document.title,
                    document.source_label,
                    document.source_uri,
                    document.content_hash,
                ),
            )
            document_id = cursor.fetchone()[0]

            cursor.execute(delete_chunks_query, (document_id,))
            cursor.executemany(
                insert_chunk_query,
                [
                    (
                        document_id,
                        chunk.chunk_index,
                        chunk.page_number,
                        chunk.content,
                        chunk.content_hash,
                        Vector(embedding),
                    )
                    for chunk, embedding in zip(document.chunks, embeddings)
                ],
            )

    return document_id
