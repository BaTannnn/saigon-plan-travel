from dataclasses import dataclass
from datetime import date

from pgvector import Vector

from app.db.postgres import get_connection


@dataclass
class SearchResult:
    place_slug: str
    place_name: str
    section: str
    content: str
    source_label: str
    source_uri: str
    similarity: float

@dataclass
class CandidateChunk:
    place_slug: str
    place_name: str
    section: str
    similarity: float
    embedding: list[float]


@dataclass
class KnowledgeChunk:
    place_slug: str
    place_name: str
    chunk_index: int
    section: str
    content: str
    source_label: str
    source_uri: str
    similarity: float | None = None


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

def search_similar_chunks(
    query_embedding: list[float],
    limit: int = 5,
) -> list[SearchResult]:
    query = """
        SELECT
            p.slug,
            p.name,
            c.section,
            c.content,
            c.source_label,
            c.source_uri,
            1 - (c.embedding <=> %s) AS similarity
        FROM place_knowledge_chunks c
        JOIN places p
            ON p.id = c.place_id
        WHERE p.active = TRUE
        ORDER BY c.embedding <=> %s
        LIMIT %s
    """

    vector = Vector(query_embedding)

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    vector,
                    vector,
                    limit,
                ),
            )

            rows = cursor.fetchall()

    return [
        SearchResult(
            place_slug=row[0],
            place_name=row[1],
            section=row[2],
            content=row[3],
            source_label=row[4],
            source_uri=row[5],
            similarity=float(row[6]),
        )
        for row in rows
    ]

def search_candidate_chunks(
    query_embedding: list[float],
    limit: int = 30,
) -> list[CandidateChunk]:
    query = """
        SELECT
            p.slug,
            p.name,
            c.section,
            1 - (c.embedding <=> %s) AS similarity,
            c.embedding
        FROM place_knowledge_chunks c
        JOIN places p
            ON p.id = c.place_id
        WHERE p.active = TRUE
        ORDER BY c.embedding <=> %s
        LIMIT %s
    """

    vector = Vector(query_embedding)

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    vector,
                    vector,
                    limit,
                ),
            )

            rows = cursor.fetchall()

    return [
        CandidateChunk(
            place_slug=row[0],
            place_name=row[1],
            section=row[2],
            similarity=float(row[3]),
            embedding=row[4].to_list(),
        )
        for row in rows
    ]


# def find_chunks_by_place_slugs(
#     place_slugs: list[str],
# ) -> list[KnowledgeChunk]:
#     if not place_slugs:
#         return []
#
#     query = """
#         SELECT
#             p.slug,
#             p.name,
#             c.chunk_index,
#             c.section,
#             c.content,
#             c.source_label,
#             c.source_uri
#         FROM place_knowledge_chunks c
#         JOIN places p
#             ON p.id = c.place_id
#         WHERE p.active = TRUE
#           AND p.slug = ANY(%s)
#         ORDER BY p.slug, c.chunk_index
#     """
#
#     with get_connection() as connection:
#         with connection.cursor() as cursor:
#             cursor.execute(query, (place_slugs,))
#             rows = cursor.fetchall()
#
#     return [
#         KnowledgeChunk(
#             place_slug=row[0],
#             place_name=row[1],
#             chunk_index=row[2],
#             section=row[3],
#             content=row[4],
#             source_label=row[5],
#             source_uri=row[6],
#         )
#         for row in rows
#     ]


def find_relevant_chunks_by_place_slugs(
    *,
    query_embedding: list[float],
    place_slugs: list[str],
    chunks_per_place: int = 2,
) -> list[KnowledgeChunk]:
    if not place_slugs:
        return []

    if chunks_per_place <= 0:
        raise ValueError("chunks_per_place must be greater than zero")

    query = """
        WITH ranked_chunks AS (
            SELECT
                p.slug AS place_slug,
                p.name AS place_name,
                c.chunk_index,
                c.section,
                c.content,
                c.source_label,
                c.source_uri,
                1 - (c.embedding <=> %s) AS similarity,
                ROW_NUMBER() OVER (
                    PARTITION BY p.id
                    ORDER BY
                        c.embedding <=> %s,
                        c.chunk_index
                ) AS evidence_rank
            FROM place_knowledge_chunks c
            JOIN places p
                ON p.id = c.place_id
            WHERE p.active = TRUE
              AND p.slug = ANY(%s)
        )
        SELECT
            place_slug,
            place_name,
            chunk_index,
            section,
            content,
            source_label,
            source_uri,
            similarity
        FROM ranked_chunks
        WHERE evidence_rank <= %s
        ORDER BY place_slug, evidence_rank
    """

    vector = Vector(query_embedding)

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                query,
                (
                    vector,
                    vector,
                    place_slugs,
                    chunks_per_place,
                ),
            )
            rows = cursor.fetchall()

    return [
        KnowledgeChunk(
            place_slug=row[0],
            place_name=row[1],
            chunk_index=row[2],
            section=row[3],
            content=row[4],
            source_label=row[5],
            source_uri=row[6],
            similarity=float(row[7]),
        )
        for row in rows
    ]
