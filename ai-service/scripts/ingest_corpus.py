from enum import Enum
from pathlib import Path

from app.rag.corpus_schema import KnowledgeChunk, PlaceCorpus
from app.rag.embedding_service import (
    compute_document_fingerprint,
    embed_document,
)
from app.rag.knowledge_repository import (
    StoredChunkState,
    delete_stale_chunks,
    find_stored_chunk_state,
    update_chunk_metadata,
    upsert_chunk,
)
from app.rag.place_lookup import resolve_place_id
from app.db.postgres import pool

CORPUS_DIR = Path("corpus")


class ChunkIngestionStatus(Enum):
    SKIPPED = "SKIP"
    METADATA_UPDATED = "METADATA"
    UPSERTED = "UPSERT"


def ingest_chunk(
    *,
    corpus: PlaceCorpus,
    chunk: KnowledgeChunk,
    place_id: int,
) -> ChunkIngestionStatus:
    title = _chunk_title(corpus, chunk)
    content_hash = compute_document_fingerprint(
        content=chunk.content,
        title=title,
    )

    stored_chunk = find_stored_chunk_state(
        place_id=place_id,
        chunk_index=chunk.chunkIndex,
    )

    if (
        stored_chunk is not None
        and stored_chunk.content_hash == content_hash
    ):
        if _metadata_is_unchanged(stored_chunk, corpus, chunk):
            return ChunkIngestionStatus.SKIPPED

        update_chunk_metadata(
            place_id=place_id,
            chunk_index=chunk.chunkIndex,
            source_label=chunk.source.label,
            source_uri=str(chunk.source.uri),
            retrieved_at=chunk.source.retrievedAt,
            language=corpus.language,
        )
        return ChunkIngestionStatus.METADATA_UPDATED

    embedding = embed_document(
        content=chunk.content,
        title=title,
    )

    changed = upsert_chunk(
        place_id=place_id,
        chunk_index=chunk.chunkIndex,
        section=chunk.section,
        content=chunk.content,
        source_label=chunk.source.label,
        source_uri=str(chunk.source.uri),
        retrieved_at=chunk.source.retrievedAt,
        language=corpus.language,
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
) -> tuple[list[ChunkIngestionStatus], int]:
    statuses = [
        ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=place_id,
        )
        for chunk in corpus.sections
    ]

    deleted = delete_stale_chunks(
        place_id=place_id,
        current_chunk_indexes=[
            chunk.chunkIndex
            for chunk in corpus.sections
        ],
    )

    return statuses, deleted


def _chunk_title(
    corpus: PlaceCorpus,
    chunk: KnowledgeChunk,
) -> str:
    return f"{corpus.placeSlug} - {chunk.section}"


def _metadata_is_unchanged(
    stored_chunk: StoredChunkState,
    corpus: PlaceCorpus,
    chunk: KnowledgeChunk,
) -> bool:
    return (
        stored_chunk.source_label == chunk.source.label
        and stored_chunk.source_uri == str(chunk.source.uri)
        and stored_chunk.retrieved_at == chunk.source.retrievedAt
        and stored_chunk.language == corpus.language
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

        place_id = resolve_place_id(
            corpus.placeSlug
        )

        if place_id is None:
            raise RuntimeError(
                f"Place not found: {corpus.placeSlug}"
            )

        print()
        print(f"[PLACE] {corpus.placeSlug}")

        statuses, deleted = ingest_place(
            corpus=corpus,
            place_id=place_id,
        )

        for chunk, status in zip(corpus.sections, statuses):
            print(
                f"  [{status.value}] "
                f"{chunk.chunkIndex} "
                f"{chunk.section}"
            )

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
