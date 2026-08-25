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


def find_chunks_by_place_slugs(
    place_slugs: list[str],
) -> list[KnowledgeChunk]:
    if not place_slugs:
        return []

    query = """
        SELECT
            p.slug,
            p.name,
            c.chunk_index,
            c.section,
            c.content,
            c.source_label,
            c.source_uri
        FROM place_knowledge_chunks c
        JOIN places p
            ON p.id = c.place_id
        WHERE p.active = TRUE
          AND p.slug = ANY(%s)
        ORDER BY p.slug, c.chunk_index
    """

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(query, (place_slugs,))
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
        )
        for row in rows
    ]
