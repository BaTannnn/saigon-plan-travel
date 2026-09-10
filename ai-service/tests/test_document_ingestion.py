from pathlib import Path

from app.knowledge.document_ingestion_repository import StoredDocumentState
from app.knowledge.pdf_corpus import (
    ExtractedPdfPage,
    PreparedDocumentChunk,
    PreparedPdfDocument,
)
from scripts.ingest_documents import DocumentIngestionStatus, ingest_pdf


def make_document(*, content_hash: str, content: str = "Nội dung") -> PreparedPdfDocument:
    return PreparedPdfDocument(
        title="Cẩm nang TP.HCM",
        source_label="Cẩm nang TP.HCM",
        source_uri="corpus/documents/guide.pdf",
        content_hash=content_hash,
        total_pages=1,
        pages=[ExtractedPdfPage(page_number=1, content=content)],
        chunks=[
            PreparedDocumentChunk(
                chunk_index=1,
                page_number=1,
                content=content,
                content_hash="c" * 64,
            )
        ],
    )


def test_duplicate_ingestion_skips_embedding_and_database_refresh(tmp_path: Path) -> None:
    corpus_dir = tmp_path / "corpus" / "documents"
    corpus_dir.mkdir(parents=True)
    path = corpus_dir / "guide.pdf"
    path.write_bytes(b"same")
    document = make_document(content_hash="a" * 64)

    def must_not_run(**_kwargs):
        raise AssertionError("unchanged documents must not be embedded or replaced")

    result = ingest_pdf(
        path,
        corpus_dir=corpus_dir,
        prepare_fn=lambda *_args, **_kwargs: document,
        find_state_fn=lambda **_kwargs: StoredDocumentState(1, "a" * 64),
        embed_fn=must_not_run,
        replace_fn=must_not_run,
    )

    assert result.status is DocumentIngestionStatus.SKIPPED
    assert result.chunks_created == 1
    assert result.chunks_embedded == 0


def test_changed_document_reembeds_and_replaces_all_chunks(tmp_path: Path) -> None:
    corpus_dir = tmp_path / "corpus" / "documents"
    corpus_dir.mkdir(parents=True)
    path = corpus_dir / "guide.pdf"
    path.write_bytes(b"changed")
    document = make_document(content_hash="b" * 64, content="Nội dung mới")
    calls: dict[str, object] = {}

    def embed(**kwargs) -> list[float]:
        calls["embed"] = kwargs
        return [0.25, 0.75]

    def replace(**kwargs) -> int:
        calls["replace"] = kwargs
        return 1

    result = ingest_pdf(
        path,
        corpus_dir=corpus_dir,
        prepare_fn=lambda *_args, **_kwargs: document,
        find_state_fn=lambda **_kwargs: StoredDocumentState(1, "a" * 64),
        embed_fn=embed,
        replace_fn=replace,
    )

    assert result.status is DocumentIngestionStatus.REFRESHED
    assert result.chunks_embedded == 1
    assert calls["embed"] == {
        "content": "Nội dung mới",
        "title": "Cẩm nang TP.HCM",
    }
    assert calls["replace"] == {
        "document": document,
        "embeddings": [[0.25, 0.75]],
    }


def test_new_document_is_created_with_repository_relative_source_uri(tmp_path: Path) -> None:
    corpus_dir = tmp_path / "corpus" / "documents"
    corpus_dir.mkdir(parents=True)
    path = corpus_dir / "guide.pdf"
    path.write_bytes(b"new")
    document = make_document(content_hash="d" * 64)
    captured: dict[str, str] = {}

    def prepare(_path: Path, *, source_uri: str) -> PreparedPdfDocument:
        captured["source_uri"] = source_uri
        return document

    result = ingest_pdf(
        path,
        corpus_dir=corpus_dir,
        prepare_fn=prepare,
        find_state_fn=lambda **_kwargs: None,
        embed_fn=lambda **_kwargs: [1.0],
        replace_fn=lambda **_kwargs: 1,
    )

    assert result.status is DocumentIngestionStatus.CREATED
    assert captured["source_uri"] == "corpus/documents/guide.pdf"
