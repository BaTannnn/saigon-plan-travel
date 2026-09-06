from dataclasses import dataclass
from datetime import date

from pgvector import Vector

from app.db.postgres import get_connection


@dataclass(frozen=True)
class StoredChunkState:
    content_hash: str
    source_label: str
    source_uri: str
    retrieved_at: date
    language: str


def find_stored_chunk_state(
    *,
    place_id: int,
    chunk_index: int,
) -> StoredChunkState | None:
    query = """
        SELECT
            content_hash,
            source_label,
            source_uri,
            retrieved_at,
            language
        FROM place_knowledge_chunks
        WHERE place_id = %s
          AND chunk_index = %s
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    place_id,
                    chunk_index,
                ),
            )

            row = cursor.fetchone()

    if row is None:
        return None

    return StoredChunkState(
        content_hash=row[0],
        source_label=row[1],
        source_uri=row[2],
        retrieved_at=row[3],
        language=row[4],
    )


def update_chunk_metadata(
    *,
    place_id: int,
    chunk_index: int,
    source_label: str,
    source_uri: str,
    retrieved_at: date,
    language: str,
) -> bool:
    query = """
        UPDATE place_knowledge_chunks
        SET
            source_label = %s,
            source_uri = %s,
            retrieved_at = %s,
            language = %s,
            updated_at = CURRENT_TIMESTAMP
        WHERE place_id = %s
          AND chunk_index = %s
          AND (
              source_label IS DISTINCT FROM %s
              OR source_uri IS DISTINCT FROM %s
              OR retrieved_at IS DISTINCT FROM %s
              OR language IS DISTINCT FROM %s
          )
    """

    parameters = (
        source_label,
        source_uri,
        retrieved_at,
        language,
        place_id,
        chunk_index,
        source_label,
        source_uri,
        retrieved_at,
        language,
    )

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(query, parameters)
            changed = cursor.rowcount > 0

    return changed


def delete_stale_chunks(
    *,
    place_id: int,
    current_chunk_indexes: list[int],
) -> int:
    if not current_chunk_indexes:
        raise ValueError("Current chunk indexes must not be empty")

    query = """
        DELETE FROM place_knowledge_chunks
        WHERE place_id = %s
          AND NOT (chunk_index = ANY(%s))
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    place_id,
                    current_chunk_indexes,
                ),
            )
            deleted = cursor.rowcount

    return deleted


def upsert_chunk(
    *,
    place_id: int,
    chunk_index: int,
    section: str,
    content: str,
    source_label: str,
    source_uri: str,
    retrieved_at: date,
    language: str,
    content_hash: str,
    embedding: list[float],
) -> bool:
    query = """
        INSERT INTO place_knowledge_chunks (
            place_id,
            chunk_index,
            section,
            content,
            source_label,
            source_uri,
            retrieved_at,
            language,
            content_hash,
            embedding
        )
        VALUES (
            %s, %s, %s, %s, %s,
            %s, %s, %s, %s, %s
        )
        ON CONFLICT (place_id, chunk_index)
        DO UPDATE SET
            section = EXCLUDED.section,
            content = EXCLUDED.content,
            source_label = EXCLUDED.source_label,
            source_uri = EXCLUDED.source_uri,
            retrieved_at = EXCLUDED.retrieved_at,
            language = EXCLUDED.language,
            content_hash = EXCLUDED.content_hash,
            embedding = EXCLUDED.embedding,
            updated_at = CURRENT_TIMESTAMP
        WHERE place_knowledge_chunks.content_hash
              IS DISTINCT FROM EXCLUDED.content_hash
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    place_id,
                    chunk_index,
                    section,
                    content,
                    source_label,
                    source_uri,
                    retrieved_at,
                    language,
                    content_hash,
                    Vector(embedding),
                ),
            )

            changed = cursor.rowcount > 0

    return changed
