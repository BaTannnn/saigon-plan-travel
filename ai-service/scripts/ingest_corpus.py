from enum import Enum
from pathlib import Path

from app.db.postgres import pool
from app.knowledge.chunking import (
    PreparedKnowledgeChunk,
    split_place_corpus,
)
from app.knowledge.corpus_schema import PlaceCorpus
from app.knowledge.embedding_service import (
    compute_document_fingerprint,
    embed_document,
)
from app.knowledge.ingestion_repository import (
    StoredChunkState,
    delete_stale_chunks,
    find_stored_chunk_state,
    update_chunk_metadata,
    upsert_chunk,
)
from app.knowledge.place_lookup import resolve_place_id

CORPUS_DIR = Path("corpus")


class ChunkIngestionStatus(Enum):
    SKIPPED = "SKIP"
    METADATA_UPDATED = "METADATA"
    UPSERTED = "UPSERT"


def ingest_chunk(
    *,
    chunk: PreparedKnowledgeChunk,
    place_id: int,
) -> ChunkIngestionStatus:
    title = _chunk_title(chunk)
    content_hash = compute_document_fingerprint(
        content=chunk.content,
        title=title,
    )

    stored_chunk = find_stored_chunk_state(
        place_id=place_id,
        chunk_index=chunk.chunk_index,
    )

    if stored_chunk is not None and stored_chunk.content_hash == content_hash:
        if _metadata_is_unchanged(stored_chunk, chunk):
            return ChunkIngestionStatus.SKIPPED

        update_chunk_metadata(
            place_id=place_id,
            chunk_index=chunk.chunk_index,
            source_label=chunk.source.label,
            source_uri=str(chunk.source.uri),
            retrieved_at=chunk.source.retrievedAt,
            language=chunk.language,
        )
        return ChunkIngestionStatus.METADATA_UPDATED

    embedding = embed_document(
        content=chunk.content,
        title=title,
    )

    changed = upsert_chunk(
        place_id=place_id,
        chunk_index=chunk.chunk_index,
        section=chunk.section,
        content=chunk.content,
        source_label=chunk.source.label,
        source_uri=str(chunk.source.uri),
        retrieved_at=chunk.source.retrievedAt,
        language=chunk.language,
        content_hash=content_hash,
        embedding=embedding,
    )

    if changed:
        return ChunkIngestionStatus.UPSERTED

    return ChunkIngestionStatus.SKIPPED


def ingest_place(
    *,
    corpus: PlaceCorpus,
    place_id: int,
) -> tuple[
    list[PreparedKnowledgeChunk],
    list[ChunkIngestionStatus],
    int,
]:
    prepared_chunks = split_place_corpus(corpus)
    statuses = [
        ingest_chunk(
            chunk=chunk,
            place_id=place_id,
        )
        for chunk in prepared_chunks
    ]

    deleted = delete_stale_chunks(
        place_id=place_id,
        current_chunk_indexes=[chunk.chunk_index for chunk in prepared_chunks],
    )

    return prepared_chunks, statuses, deleted


def _chunk_title(
    chunk: PreparedKnowledgeChunk,
) -> str:
    return f"{chunk.place_slug} - {chunk.section}"


def _metadata_is_unchanged(
    stored_chunk: StoredChunkState,
    chunk: PreparedKnowledgeChunk,
) -> bool:
    return (
        stored_chunk.source_label == chunk.source.label
        and stored_chunk.source_uri == str(chunk.source.uri)
        and stored_chunk.retrieved_at == chunk.source.retrievedAt
        and stored_chunk.language == chunk.language
    )


def main() -> None:
    files = sorted(CORPUS_DIR.glob("*.json"))

    if not files:
        print("No corpus files found.")
        return

    inserted_or_updated = 0
    metadata_updated = 0
    skipped = 0
    stale_deleted = 0

    for path in files:
        raw_json = path.read_text(encoding="utf-8")

        corpus = PlaceCorpus.model_validate_json(raw_json)

        place_id = resolve_place_id(corpus.placeSlug)

        if place_id is None:
            raise RuntimeError(f"Place not found: {corpus.placeSlug}")

        print()
        print(f"[PLACE] {corpus.placeSlug}")

        prepared_chunks, statuses, deleted = ingest_place(
            corpus=corpus,
            place_id=place_id,
        )

        for chunk, status in zip(prepared_chunks, statuses):
            print(f"  [{status.value}] {chunk.chunk_index} {chunk.section}")

            if status is ChunkIngestionStatus.UPSERTED:
                inserted_or_updated += 1
            elif status is ChunkIngestionStatus.METADATA_UPDATED:
                metadata_updated += 1
            else:
                skipped += 1

        if deleted:
            print(f"  [DELETE] {deleted} stale chunk(s)")
            stale_deleted += deleted

    print()
    print(f"Changed: {inserted_or_updated}")
    print(f"Metadata updated: {metadata_updated}")
    print(f"Skipped: {skipped}")
    print(f"Stale deleted: {stale_deleted}")


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()
