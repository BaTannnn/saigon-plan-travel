import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from app.knowledge.chunking import split_place_corpus
from scripts.evaluate_chunking import (
    CONFIGURATION_ORDER,
    CORPUS_DIR,
    MARKDOWN_HEADER,
    RECURSIVE_500,
    RECURSIVE_500_CHUNK_OVERLAP,
    RECURSIVE_500_CHUNK_SIZE,
    RECURSIVE_800,
    RECURSIVE_800_CHUNK_OVERLAP,
    RECURSIVE_800_CHUNK_SIZE,
    SAMPLE_PLACE_SLUGS,
    SampleChunk,
    build_configurations,
    build_markdown_report,
    calculate_statistics,
    load_sample_corpora,
    split_recursive_text,
    write_markdown_report,
)


class ChunkingExperimentTest(unittest.TestCase):

    def test_loads_only_explicit_real_corpus_samples(self):
        corpora = load_sample_corpora(CORPUS_DIR)

        self.assertEqual(
            list(SAMPLE_PLACE_SLUGS),
            [corpus.placeSlug for corpus in corpora],
        )
        self.assertGreaterEqual(len(corpora), 5)
        self.assertLessEqual(len(corpora), 10)

    def test_builds_three_non_empty_configurations(self):
        corpus = load_sample_corpora()[0]

        configurations = build_configurations(corpus)

        self.assertEqual(list(CONFIGURATION_ORDER), list(configurations))
        for chunks in configurations.values():
            self.assertTrue(chunks)
            self.assertTrue(all(chunk.content.strip() for chunk in chunks))

    def test_recursive_500_uses_500_characters_with_50_overlap(self):
        text = "".join(chr(0xE000 + index) for index in range(1200))

        chunks = split_recursive_text(
            text,
            chunk_size=RECURSIVE_500_CHUNK_SIZE,
            chunk_overlap=RECURSIVE_500_CHUNK_OVERLAP,
        )

        self.assertEqual((500, 50), (
            RECURSIVE_500_CHUNK_SIZE,
            RECURSIVE_500_CHUNK_OVERLAP,
        ))
        self.assertGreater(len(chunks), 1)
        self.assertEqual(500, len(chunks[0].content))
        self.assertEqual(
            chunks[0].content[-50:],
            chunks[1].content[:50],
        )

    def test_recursive_800_uses_800_characters_with_100_overlap(self):
        text = "".join(chr(0xE000 + index) for index in range(1700))

        chunks = split_recursive_text(
            text,
            chunk_size=RECURSIVE_800_CHUNK_SIZE,
            chunk_overlap=RECURSIVE_800_CHUNK_OVERLAP,
        )

        self.assertEqual((800, 100), (
            RECURSIVE_800_CHUNK_SIZE,
            RECURSIVE_800_CHUNK_OVERLAP,
        ))
        self.assertGreater(len(chunks), 1)
        self.assertEqual(800, len(chunks[0].content))
        self.assertEqual(
            chunks[0].content[-100:],
            chunks[1].content[:100],
        )

    def test_markdown_configuration_reuses_production_chunks(self):
        corpus = load_sample_corpora()[0]

        with patch(
            "scripts.evaluate_chunking.split_place_corpus",
            wraps=split_place_corpus,
        ) as production_splitter:
            chunks = build_configurations(corpus)[MARKDOWN_HEADER]

        production_splitter.assert_called_once_with(corpus)
        self.assertEqual(
            [section.section for section in corpus.sections],
            [chunk.section for chunk in chunks],
        )
        self.assertEqual(4, len(chunks))

    def test_calculates_simple_character_statistics(self):
        statistics = calculate_statistics(
            [
                SampleChunk(content="a" * 10),
                SampleChunk(content="b" * 20),
                SampleChunk(content="c" * 30),
            ]
        )

        self.assertEqual(3, statistics.chunk_count)
        self.assertEqual(10, statistics.minimum_length)
        self.assertEqual(20, statistics.average_length)
        self.assertEqual(30, statistics.maximum_length)

    def test_report_contains_samples_configurations_rubric_and_sections(self):
        corpora = load_sample_corpora()

        report, summary = build_markdown_report(corpora)

        self.assertEqual(
            list(CONFIGURATION_ORDER),
            list(summary),
        )
        self.assertEqual(
            list(SAMPLE_PLACE_SLUGS),
            [
                line.removeprefix("## Place: ")
                for line in report.splitlines()
                if line.startswith("## Place: ")
            ],
        )
        self.assertIn("Manual Evaluation Rubric", report)
        self.assertIn("Recursive 500/50", report)
        self.assertIn("Recursive 800/100", report)
        self.assertIn("MarkdownHeader + Recursive 800/100", report)
        self.assertIn("Section: `OVERVIEW`", report)
        self.assertIn("## Final Summary", report)

    def test_runs_offline_without_embeddings_or_database(self):
        corpus = load_sample_corpora()[0]

        with patch(
            "app.knowledge.embedding_service.embed_document"
        ) as embed_document, patch(
            "app.knowledge.embedding_service.embed_query"
        ) as embed_query, patch(
            "app.knowledge.knowledge_repository.get_connection"
        ) as get_connection, patch(
            "app.knowledge.knowledge_repository.upsert_chunk"
        ) as upsert_chunk, patch(
            "app.knowledge.knowledge_repository.delete_stale_chunks"
        ) as delete_stale_chunks:
            report, _ = build_markdown_report([corpus])

            with TemporaryDirectory() as temporary_directory:
                output_path = Path(temporary_directory) / "samples.md"
                write_markdown_report(
                    output_path=output_path,
                    report=report,
                )
                self.assertTrue(output_path.read_text(encoding="utf-8"))

        embed_document.assert_not_called()
        embed_query.assert_not_called()
        get_connection.assert_not_called()
        upsert_chunk.assert_not_called()
        delete_stale_chunks.assert_not_called()


if __name__ == "__main__":
    unittest.main()
