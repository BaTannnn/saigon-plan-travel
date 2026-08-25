import unittest
from datetime import date
from unittest.mock import MagicMock, patch

from app.rag.knowledge_repository import (
    delete_stale_chunks,
    find_relevant_chunks_by_place_slugs,
    find_stored_chunk_state,
    update_chunk_metadata,
)


class KnowledgeRepositoryTest(unittest.TestCase):

    @patch("app.rag.knowledge_repository.get_connection")
    def test_finds_stored_chunk_state_by_existing_chunk_identity(
        self,
        get_connection: MagicMock,
    ):
        cursor = (
            get_connection.return_value.__enter__.return_value
            .cursor.return_value.__enter__.return_value
        )
        cursor.fetchone.return_value = (
            "abc",
            "Source",
            "https://example.com",
            date(2026, 8, 25),
            "vi-VN",
        )

        state = find_stored_chunk_state(
            place_id=42,
            chunk_index=3,
        )

        self.assertEqual("abc", state.content_hash)
        self.assertEqual("Source", state.source_label)
        query, parameters = cursor.execute.call_args.args
        self.assertIn("place_id = %s", query)
        self.assertIn("chunk_index = %s", query)
        self.assertEqual((42, 3), parameters)

    @patch("app.rag.knowledge_repository.get_connection")
    def test_missing_chunk_has_no_stored_state(
        self,
        get_connection: MagicMock,
    ):
        cursor = (
            get_connection.return_value.__enter__.return_value
            .cursor.return_value.__enter__.return_value
        )
        cursor.fetchone.return_value = None

        state = find_stored_chunk_state(
            place_id=42,
            chunk_index=3,
        )

        self.assertIsNone(state)

    @patch("app.rag.knowledge_repository.get_connection")
    def test_updates_only_metadata_without_vector(
        self,
        get_connection: MagicMock,
    ):
        cursor = (
            get_connection.return_value.__enter__.return_value
            .cursor.return_value.__enter__.return_value
        )
        cursor.rowcount = 1

        changed = update_chunk_metadata(
            place_id=42,
            chunk_index=3,
            source_label="Updated source",
            source_uri="https://example.com/updated",
            retrieved_at=date(2026, 8, 25),
            language="vi-VN",
        )

        self.assertTrue(changed)
        query, parameters = cursor.execute.call_args.args
        self.assertNotIn("embedding", query)
        self.assertIn("place_id = %s", query)
        self.assertEqual(42, parameters[4])
        self.assertEqual(3, parameters[5])

    @patch("app.rag.knowledge_repository.get_connection")
    def test_deletes_stale_chunks_only_for_current_place(
        self,
        get_connection: MagicMock,
    ):
        cursor = (
            get_connection.return_value.__enter__.return_value
            .cursor.return_value.__enter__.return_value
        )
        cursor.rowcount = 2

        deleted = delete_stale_chunks(
            place_id=42,
            current_chunk_indexes=[1, 2, 3],
        )

        self.assertEqual(2, deleted)
        query, parameters = cursor.execute.call_args.args
        self.assertIn("WHERE place_id = %s", query)
        self.assertIn("NOT (chunk_index = ANY(%s))", query)
        self.assertEqual((42, [1, 2, 3]), parameters)

    @patch("app.rag.knowledge_repository.get_connection")
    def test_refuses_stale_cleanup_with_empty_current_indexes(
        self,
        get_connection: MagicMock,
    ):
        with self.assertRaisesRegex(ValueError, "must not be empty"):
            delete_stale_chunks(
                place_id=42,
                current_chunk_indexes=[],
            )

        get_connection.assert_not_called()

    @patch("app.rag.knowledge_repository.get_connection")
    def test_finds_ranked_evidence_for_requested_places_in_one_query(
        self,
        get_connection: MagicMock,
    ):
        cursor = (
            get_connection.return_value.__enter__.return_value
            .cursor.return_value.__enter__.return_value
        )
        cursor.fetchall.return_value = [
            (
                "place-a",
                "Place A",
                2,
                "HIGHLIGHTS",
                "Relevant context A",
                "Source A",
                "https://example.com/a",
                0.91,
            ),
            (
                "place-b",
                "Place B",
                4,
                "EXPERIENCE",
                "Relevant context B",
                "Source B",
                "https://example.com/b",
                0.88,
            ),
        ]

        chunks = find_relevant_chunks_by_place_slugs(
            query_embedding=[0.1, 0.2],
            place_slugs=["place-a", "place-b"],
            chunks_per_place=2,
        )

        self.assertEqual(
            ["place-a", "place-b"],
            [chunk.place_slug for chunk in chunks],
        )
        self.assertEqual([0.91, 0.88], [chunk.similarity for chunk in chunks])
        get_connection.assert_called_once()
        cursor.execute.assert_called_once()
        query, parameters = cursor.execute.call_args.args
        self.assertIn("p.active = TRUE", query)
        self.assertIn("p.slug = ANY(%s)", query)
        self.assertIn("ROW_NUMBER() OVER", query)
        self.assertIn("PARTITION BY p.id", query)
        self.assertIn("c.embedding <=> %s", query)
        self.assertIn("evidence_rank <= %s", query)
        self.assertEqual(["place-a", "place-b"], parameters[2])
        self.assertEqual(2, parameters[3])

    @patch("app.rag.knowledge_repository.get_connection")
    def test_empty_relevant_place_list_does_not_query_database(
        self,
        get_connection: MagicMock,
    ):
        chunks = find_relevant_chunks_by_place_slugs(
            query_embedding=[0.1, 0.2],
            place_slugs=[],
            chunks_per_place=2,
        )

        self.assertEqual([], chunks)
        get_connection.assert_not_called()


if __name__ == "__main__":
    unittest.main()
