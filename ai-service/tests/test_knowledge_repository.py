import unittest
from unittest.mock import MagicMock, patch

from app.rag.knowledge_repository import find_chunks_by_place_slugs


class KnowledgeRepositoryTest(unittest.TestCase):

    @patch("app.rag.knowledge_repository.get_connection")
    def test_finds_only_chunks_for_requested_place_slugs(
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
                1,
                "OVERVIEW",
                "Context A",
                "Source A",
                "https://example.com/a",
            ),
            (
                "place-b",
                "Place B",
                2,
                "HIGHLIGHTS",
                "Context B",
                "Source B",
                "https://example.com/b",
            ),
        ]

        chunks = find_chunks_by_place_slugs(["place-a", "place-b"])

        self.assertEqual(
            ["place-a", "place-b"],
            [chunk.place_slug for chunk in chunks],
        )
        query, parameters = cursor.execute.call_args.args
        self.assertIn("p.slug = ANY(%s)", query)
        self.assertIn("ORDER BY p.slug, c.chunk_index", query)
        self.assertEqual((["place-a", "place-b"],), parameters)

    @patch("app.rag.knowledge_repository.get_connection")
    def test_empty_slug_list_does_not_query_database(
        self,
        get_connection: MagicMock,
    ):
        self.assertEqual([], find_chunks_by_place_slugs([]))
        get_connection.assert_not_called()


if __name__ == "__main__":
    unittest.main()
