import unittest
from unittest.mock import MagicMock, patch

from app.rag.itinerary_explanation_service import (
    GeneratedItineraryReason,
    GeneratedItineraryReasons,
    INSUFFICIENT_CONTEXT_REASON,
    generate_itinerary_reasons,
)
from app.rag.knowledge_repository import KnowledgeChunk


def chunk(slug: str, content: str) -> KnowledgeChunk:
    return KnowledgeChunk(
        place_slug=slug,
        place_name=slug,
        chunk_index=1,
        section="OVERVIEW",
        content=content,
        source_label="Test source",
        source_uri=f"https://example.com/{slug}",
    )


class ItineraryExplanationServiceTest(unittest.TestCase):

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch("app.rag.itinerary_explanation_service.find_chunks_by_place_slugs")
    def test_generates_multiple_grounded_reasons_with_one_model_call(
        self,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [
            chunk("place-a", "Có không gian xanh và nhiều cây."),
            chunk("place-b", "Trưng bày nghệ thuật hiện đại."),
        ]
        structured_model = (
            create_model.return_value.with_structured_output.return_value
        )
        structured_model.invoke.return_value = GeneratedItineraryReasons(
            reasons=[
                GeneratedItineraryReason(
                    place_slug="place-a",
                    reason="Phù hợp với sở thích thiên nhiên.",
                ),
                GeneratedItineraryReason(
                    place_slug="place-b",
                    reason="Phù hợp với sở thích nghệ thuật.",
                ),
            ]
        )

        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên và nghệ thuật",
            ["place-a", "place-b"],
        )

        self.assertEqual(
            ["place-a", "place-b"],
            [item.place_slug for item in reasons],
        )
        self.assertIn(
            "Có không gian xanh và nhiều cây.",
            structured_model.invoke.call_args.args[0][1].content,
        )
        structured_model.invoke.assert_called_once()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch("app.rag.itinerary_explanation_service.find_chunks_by_place_slugs")
    def test_returns_neutral_reason_for_place_without_knowledge(
        self,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [chunk("place-a", "Context A")]
        structured_model = (
            create_model.return_value.with_structured_output.return_value
        )
        structured_model.invoke.return_value = GeneratedItineraryReasons(
            reasons=[
                GeneratedItineraryReason(
                    place_slug="place-a",
                    reason="Lý do A",
                ),
                GeneratedItineraryReason(
                    place_slug="place-without-context",
                    reason="This ungrounded text must not be used",
                ),
            ]
        )

        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên",
            ["place-a", "place-without-context"],
        )

        self.assertEqual("Lý do A", reasons[0].reason)
        self.assertEqual(INSUFFICIENT_CONTEXT_REASON, reasons[1].reason)
        prompt = structured_model.invoke.call_args.args[0][1].content
        self.assertIn("place-without-context", prompt)
        structured_model.invoke.assert_called_once()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch("app.rag.itinerary_explanation_service.find_chunks_by_place_slugs")
    def test_returns_neutral_reasons_without_model_when_no_knowledge_exists(
        self,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = []

        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên",
            ["missing-place"],
        )

        self.assertEqual(INSUFFICIENT_CONTEXT_REASON, reasons[0].reason)
        create_model.assert_not_called()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch("app.rag.itinerary_explanation_service.find_chunks_by_place_slugs")
    def test_rejects_malformed_model_output(
        self,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [chunk("place-a", "Context A")]
        structured_model = (
            create_model.return_value.with_structured_output.return_value
        )
        structured_model.invoke.return_value = {"unexpected": []}

        with self.assertRaisesRegex(
            ValueError,
            "Malformed itinerary explanation output",
        ):
            generate_itinerary_reasons(
                "Tôi thích thiên nhiên",
                ["place-a"],
            )


if __name__ == "__main__":
    unittest.main()
