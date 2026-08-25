import unittest
from datetime import date
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import MagicMock, patch

from pydantic import ValidationError

from app.rag.corpus_schema import KnowledgeChunk, PlaceCorpus, Source
from app.rag.embedding_service import compute_document_fingerprint
from app.rag.knowledge_repository import StoredChunkState
from scripts.ingest_corpus import (
    ChunkIngestionStatus,
    ingest_chunk,
    ingest_place,
    main,
)


CONTENT = (
    "Nội dung thử nghiệm đủ dài để vượt qua giới hạn tối thiểu "
    "của một knowledge chunk trong corpus."
)
CHANGED_CONTENT = (
    "Nội dung đã thay đổi và vẫn đủ dài để vượt qua giới hạn tối thiểu "
    "của một knowledge chunk trong corpus."
)


def corpus_and_chunk(
    *,
    section: str = "OVERVIEW",
    content: str = CONTENT,
    source_label: str = "Test source",
) -> tuple[PlaceCorpus, KnowledgeChunk]:
    chunk = KnowledgeChunk(
        chunkIndex=1,
        section=section,
        content=content,
        source=Source(
            label=source_label,
            uri="https://example.com/place",
            retrievedAt=date(2026, 8, 25),
        ),
    )
    corpus = PlaceCorpus(
        placeSlug="test-place",
        language="vi-VN",
        sections=[chunk],
    )
    return corpus, chunk


def stored_chunk_state(
    *,
    content_hash: str,
    source_label: str = "Test source",
) -> StoredChunkState:
    return StoredChunkState(
        content_hash=content_hash,
        source_label=source_label,
        source_uri="https://example.com/place",
        retrieved_at=date(2026, 8, 25),
        language="vi-VN",
    )


def fingerprint(
    *,
    content: str = CONTENT,
    section: str = "OVERVIEW",
) -> str:
    return compute_document_fingerprint(
        content=content,
        title=f"test-place - {section}",
    )


class IngestCorpusTest(unittest.TestCase):

    @patch("scripts.ingest_corpus.update_chunk_metadata")
    @patch("scripts.ingest_corpus.upsert_chunk")
    @patch("scripts.ingest_corpus.embed_document")
    @patch("scripts.ingest_corpus.find_stored_chunk_state")
    def test_identical_embedding_input_and_metadata_skips_all_writes(
        self,
        find_stored_chunk_state: MagicMock,
        embed_document: MagicMock,
        upsert_chunk: MagicMock,
        update_chunk_metadata: MagicMock,
    ):
        corpus, chunk = corpus_and_chunk()
        find_stored_chunk_state.return_value = stored_chunk_state(
            content_hash=fingerprint(),
        )

        status = ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=42,
        )

        self.assertIs(ChunkIngestionStatus.SKIPPED, status)
        embed_document.assert_not_called()
        upsert_chunk.assert_not_called()
        update_chunk_metadata.assert_not_called()

    @patch("scripts.ingest_corpus.upsert_chunk", return_value=True)
    @patch("scripts.ingest_corpus.embed_document", return_value=[0.1, 0.2])
    @patch("scripts.ingest_corpus.find_stored_chunk_state")
    def test_content_change_embeds_and_upserts(
        self,
        find_stored_chunk_state: MagicMock,
        embed_document: MagicMock,
        upsert_chunk: MagicMock,
    ):
        corpus, chunk = corpus_and_chunk(content=CHANGED_CONTENT)
        find_stored_chunk_state.return_value = stored_chunk_state(
            content_hash=fingerprint(content=CONTENT),
        )

        status = ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=42,
        )

        self.assertIs(ChunkIngestionStatus.UPSERTED, status)
        embed_document.assert_called_once()
        self.assertEqual(
            fingerprint(content=CHANGED_CONTENT),
            upsert_chunk.call_args.kwargs["content_hash"],
        )

    @patch("scripts.ingest_corpus.upsert_chunk", return_value=True)
    @patch("scripts.ingest_corpus.embed_document", return_value=[0.1, 0.2])
    @patch("scripts.ingest_corpus.find_stored_chunk_state")
    def test_section_title_change_with_same_content_embeds_and_upserts(
        self,
        find_stored_chunk_state: MagicMock,
        embed_document: MagicMock,
        upsert_chunk: MagicMock,
    ):
        corpus, chunk = corpus_and_chunk(section="BACKGROUND")
        find_stored_chunk_state.return_value = stored_chunk_state(
            content_hash=fingerprint(section="OVERVIEW"),
        )

        status = ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=42,
        )

        self.assertIs(ChunkIngestionStatus.UPSERTED, status)
        embed_document.assert_called_once_with(
            content=CONTENT,
            title="test-place - BACKGROUND",
        )
        upsert_chunk.assert_called_once()

    @patch("scripts.ingest_corpus.upsert_chunk", return_value=True)
    @patch("scripts.ingest_corpus.embed_document", return_value=[0.1, 0.2])
    @patch("scripts.ingest_corpus.find_stored_chunk_state", return_value=None)
    def test_new_chunk_embeds_and_inserts(
        self,
        find_stored_chunk_state: MagicMock,
        embed_document: MagicMock,
        upsert_chunk: MagicMock,
    ):
        corpus, chunk = corpus_and_chunk()

        status = ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=42,
        )

        self.assertIs(ChunkIngestionStatus.UPSERTED, status)
        embed_document.assert_called_once()
        upsert_chunk.assert_called_once()

    @patch("scripts.ingest_corpus.update_chunk_metadata", return_value=True)
    @patch("scripts.ingest_corpus.upsert_chunk")
    @patch("scripts.ingest_corpus.embed_document")
    @patch("scripts.ingest_corpus.find_stored_chunk_state")
    def test_provenance_only_change_updates_metadata_without_embedding(
        self,
        find_stored_chunk_state: MagicMock,
        embed_document: MagicMock,
        upsert_chunk: MagicMock,
        update_chunk_metadata: MagicMock,
    ):
        corpus, chunk = corpus_and_chunk(source_label="Updated source")
        find_stored_chunk_state.return_value = stored_chunk_state(
            content_hash=fingerprint(),
            source_label="Old source",
        )

        status = ingest_chunk(
            corpus=corpus,
            chunk=chunk,
            place_id=42,
        )

        self.assertIs(ChunkIngestionStatus.METADATA_UPDATED, status)
        embed_document.assert_not_called()
        upsert_chunk.assert_not_called()
        self.assertEqual(
            "Updated source",
            update_chunk_metadata.call_args.kwargs["source_label"],
        )

    @patch("scripts.ingest_corpus.delete_stale_chunks", return_value=1)
    @patch(
        "scripts.ingest_corpus.ingest_chunk",
        return_value=ChunkIngestionStatus.SKIPPED,
    )
    def test_stale_chunks_are_deleted_after_place_ingestion_succeeds(
        self,
        ingest_chunk: MagicMock,
        delete_stale_chunks: MagicMock,
    ):
        corpus, _ = corpus_and_chunk()

        statuses, deleted = ingest_place(
            corpus=corpus,
            place_id=42,
        )

        self.assertEqual([ChunkIngestionStatus.SKIPPED], statuses)
        self.assertEqual(1, deleted)
        delete_stale_chunks.assert_called_once_with(
            place_id=42,
            current_chunk_indexes=[1],
        )

    @patch("scripts.ingest_corpus.delete_stale_chunks")
    @patch(
        "scripts.ingest_corpus.ingest_chunk",
        side_effect=RuntimeError("embedding failed"),
    )
    def test_stale_chunks_are_not_deleted_after_ingestion_failure(
        self,
        ingest_chunk: MagicMock,
        delete_stale_chunks: MagicMock,
    ):
        corpus, _ = corpus_and_chunk()

        with self.assertRaisesRegex(RuntimeError, "embedding failed"):
            ingest_place(
                corpus=corpus,
                place_id=42,
            )

        delete_stale_chunks.assert_not_called()

    @patch("scripts.ingest_corpus.ingest_place")
    def test_invalid_corpus_never_reaches_ingestion_or_cleanup(
        self,
        ingest_place: MagicMock,
    ):
        with TemporaryDirectory() as temporary_directory:
            corpus_dir = Path(temporary_directory)
            (corpus_dir / "invalid.json").write_text(
                '{"placeSlug":"test-place",'
                '"language":"vi-VN","sections":[]}',
                encoding="utf-8",
            )

            with patch("scripts.ingest_corpus.CORPUS_DIR", corpus_dir):
                with self.assertRaises(ValidationError):
                    main()

        ingest_place.assert_not_called()


if __name__ == "__main__":
    unittest.main()
