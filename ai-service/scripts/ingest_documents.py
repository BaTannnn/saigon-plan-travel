from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import Callable

from app.db.postgres import pool
from app.knowledge.document_ingestion_repository import (
    StoredDocumentState,
    find_stored_document_state,
    replace_document_chunks,
)
from app.knowledge.embedding_service import embed_document
from app.knowledge.pdf_corpus import PreparedPdfDocument, prepare_pdf_document

DOCUMENT_CORPUS_DIR = Path("corpus/documents")


class DocumentIngestionStatus(Enum):
    CREATED = "CREATED"
    REFRESHED = "REFRESHED"
    SKIPPED = "SKIPPED"


@dataclass(frozen=True)
class DocumentIngestionResult:
    status: DocumentIngestionStatus
    pages_extracted: int
    chunks_created: int
    chunks_embedded: int


def ingest_pdf(
    path: Path,
    *,
    corpus_dir: Path = DOCUMENT_CORPUS_DIR,
    prepare_fn: Callable[..., PreparedPdfDocument] = prepare_pdf_document,
    find_state_fn: Callable[..., StoredDocumentState | None] = find_stored_document_state,
    embed_fn: Callable[..., list[float] | None] = embed_document,
    replace_fn: Callable[..., int] = replace_document_chunks,
) -> DocumentIngestionResult:
    relative_pdf_path = path.resolve().relative_to(corpus_dir.resolve())
    source_uri = (DOCUMENT_CORPUS_DIR / relative_pdf_path).as_posix()
    document = prepare_fn(path, source_uri=source_uri)
    stored = find_state_fn(source_uri=document.source_uri)

    if stored is not None and stored.content_hash == document.content_hash:
        return DocumentIngestionResult(
            status=DocumentIngestionStatus.SKIPPED,
            pages_extracted=document.total_pages,
            chunks_created=len(document.chunks),
            chunks_embedded=0,
        )

    embeddings: list[list[float]] = []
    for chunk in document.chunks:
        embedding = embed_fn(
            content=chunk.content,
            title=document.title,
        )
        if embedding is None:
            raise RuntimeError(
                f"Embedding provider returned no vector for chunk {chunk.chunk_index}"
            )
        embeddings.append(embedding)

    replace_fn(document=document, embeddings=embeddings)

    return DocumentIngestionResult(
        status=(
            DocumentIngestionStatus.CREATED if stored is None else DocumentIngestionStatus.REFRESHED
        ),
        pages_extracted=document.total_pages,
        chunks_created=len(document.chunks),
        chunks_embedded=len(embeddings),
    )


def main() -> None:
    files = sorted(
        path
        for path in DOCUMENT_CORPUS_DIR.iterdir()
        if path.is_file() and path.suffix.lower() == ".pdf"
    )

    if not files:
        print(f"No PDF corpus files found in {DOCUMENT_CORPUS_DIR.as_posix()}.")
        return

    processed = 0
    skipped = 0
    chunks_embedded = 0

    for path in files:
        print(f"Processing: {path.name}")
        result = ingest_pdf(path)
        print(f"Pages extracted: {result.pages_extracted}")
        print(f"Chunks created: {result.chunks_created}")
        print(f"Chunks embedded: {result.chunks_embedded}")
        print(f"Document ingestion {result.status.value.lower()}.")
        print()

        if result.status is DocumentIngestionStatus.SKIPPED:
            skipped += 1
        else:
            processed += 1
            chunks_embedded += result.chunks_embedded

    print(f"Documents changed: {processed}")
    print(f"Documents skipped: {skipped}")
    print(f"Chunks embedded: {chunks_embedded}")


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()
