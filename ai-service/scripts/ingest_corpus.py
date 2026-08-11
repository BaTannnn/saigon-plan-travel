from pathlib import Path

from app.rag.content_hash import compute_content_hash
from app.rag.corpus_schema import PlaceCorpus
from app.rag.embedding_service import embed_document
from app.rag.knowledge_repository import upsert_chunk
from app.rag.place_lookup import resolve_place_id


CORPUS_DIR = Path("corpus")


def main() -> None:
    files = sorted(CORPUS_DIR.glob("*.json"))

    if not files:
        print("No corpus files found.")
        return

    inserted_or_updated = 0
    skipped = 0

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

        for chunk in corpus.sections:
            content_hash = compute_content_hash(
                chunk.content
            )

            embedding = embed_document(
                content=chunk.content,
                title=(
                    f"{corpus.placeSlug} - "
                    f"{chunk.section}"
                ),
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
                print(
                    f"  [UPSERT] "
                    f"{chunk.chunkIndex} "
                    f"{chunk.section}"
                )

                inserted_or_updated += 1
            else:
                print(
                    f"  [SKIP] "
                    f"{chunk.chunkIndex} "
                    f"{chunk.section}"
                )

                skipped += 1

    print()
    print(f"Changed: {inserted_or_updated}")
    print(f"Skipped: {skipped}")


if __name__ == "__main__":
    main()