from pathlib import Path

from app.rag.chunking import split_place_corpus
from app.rag.corpus_schema import PlaceCorpus
from app.rag.embedding_service import compute_document_fingerprint


CORPUS_DIR = Path("corpus")


def main() -> None:
    files = sorted(CORPUS_DIR.glob("*.json"))

    if not files:
        print("No corpus files found.")
        return

    total_chunks = 0

    for path in files:
        raw_json = path.read_text(encoding="utf-8")

        corpus = PlaceCorpus.model_validate_json(raw_json)

        print()
        print(f"{corpus.placeSlug}")

        chunks = split_place_corpus(corpus)

        for chunk in chunks:
            content_hash = compute_document_fingerprint(
                content=chunk.content,
                title=f"{chunk.place_slug} - {chunk.section}",
            )

            print(
                f"  chunk={chunk.chunk_index} "
                f"section={chunk.section} "
                f"hash={content_hash}"
            )

            total_chunks += 1

    print()
    print(f"Files: {len(files)}")
    print(f"Chunks: {total_chunks}")


if __name__ == "__main__":
    main()
