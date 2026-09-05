from dataclasses import dataclass

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
        CandidateChunk(
            place_slug=row[0],
            place_name=row[1],
            section=row[2],
            similarity=float(row[3]),
        )
        for row in rows
    ]


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
