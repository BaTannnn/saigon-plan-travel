import unittest
from unittest.mock import MagicMock, patch

from app.rag.itinerary_explanation_service import (
    GeneratedItineraryReason,
    GeneratedItineraryReasons,
    INSUFFICIENT_CONTEXT_REASON,
    generate_itinerary_reasons,
)
from app.knowledge.knowledge_repository import KnowledgeChunk


def chunk(
    slug: str,
    content: str,
    *,
    chunk_index: int = 1,
    section: str = "OVERVIEW",
) -> KnowledgeChunk:
    return KnowledgeChunk(
        place_slug=slug,
        place_name=slug,
        chunk_index=chunk_index,
        section=section,
        content=content,
        source_label="Test source",
        source_uri=f"https://example.com/{slug}",
        similarity=0.9,
    )


def structured_model(create_model: MagicMock) -> MagicMock:
    return create_model.return_value.with_structured_output.return_value


class ItineraryExplanationServiceTest(unittest.TestCase):

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_embeds_preference_once_and_retrieves_requested_places_once(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [
            chunk("place-a", "Có không gian xanh và nhiều cây."),
            chunk("place-b", "Trưng bày nghệ thuật hiện đại."),
        ]
        model = structured_model(create_model)
        model.invoke.return_value = GeneratedItineraryReasons(
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
        embed_query.assert_called_once_with(
            "Tôi thích thiên nhiên và nghệ thuật"
        )
        find_chunks.assert_called_once_with(
            query_embedding=[0.1, 0.2],
            place_slugs=["place-a", "place-b"],
            chunks_per_place=2,
        )
        prompt = model.invoke.call_args.args[0][1].content
        self.assertIn("Có không gian xanh và nhiều cây.", prompt)
        self.assertIn("Trưng bày nghệ thuật hiện đại.", prompt)
        self.assertIn("Source URI: https://example.com/place-a", prompt)
        model.invoke.assert_called_once()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_context_is_bounded_and_excludes_unselected_noisy_chunks(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [
            chunk("place-a", "Selected A1", chunk_index=1),
            chunk("place-a", "Selected A2", chunk_index=2),
            chunk("place-a", "Noisy A3", chunk_index=3),
            chunk("place-b", "Selected B1", chunk_index=1),
            chunk("place-b", "Selected B2", chunk_index=2),
            chunk("place-b", "Noisy B3", chunk_index=3),
            chunk("not-requested", "Unselected noisy context"),
        ]
        model = structured_model(create_model)
        model.invoke.return_value = GeneratedItineraryReasons(
            reasons=[
                GeneratedItineraryReason(
                    place_slug="place-a",
                    reason="Reason A",
                ),
                GeneratedItineraryReason(
                    place_slug="place-b",
                    reason="Reason B",
                ),
            ]
        )

        generate_itinerary_reasons(
            "Preference",
            ["place-a", "place-b"],
            chunks_per_place=2,
        )

        prompt = model.invoke.call_args.args[0][1].content
        self.assertEqual(4, prompt.count("[CHUNK "))
        self.assertIn("Selected A1", prompt)
        self.assertIn("Selected A2", prompt)
        self.assertIn("Selected B1", prompt)
        self.assertIn("Selected B2", prompt)
        self.assertNotIn("Noisy A3", prompt)
        self.assertNotIn("Noisy B3", prompt)
        self.assertNotIn("Unselected noisy context", prompt)

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_sends_only_evidenced_places_and_merges_missing_context(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [
            chunk("place-a", "Context A"),
            chunk("place-b", "Context B"),
        ]
        model = structured_model(create_model)
        model.invoke.return_value = GeneratedItineraryReasons(
            reasons=[
                GeneratedItineraryReason(
                    place_slug="place-b",
                    reason="Lý do B",
                ),
                GeneratedItineraryReason(
                    place_slug="place-a",
                    reason="Lý do A",
                ),
            ]
        )

        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên",
            ["place-a", "place-b", "place-c"],
        )

        self.assertEqual(
            ["place-a", "place-b", "place-c"],
            [item.place_slug for item in reasons],
        )
        self.assertEqual(
            ["Lý do A", "Lý do B", INSUFFICIENT_CONTEXT_REASON],
            [item.reason for item in reasons],
        )
        prompt = model.invoke.call_args.args[0][1].content
        self.assertIn(
            "PLACE SLUGS:\nplace-a\nplace-b\n\nCONTEXT:",
            prompt,
        )
        self.assertNotIn("place-c", prompt)
        model.invoke.assert_called_once()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs",
        return_value=[],
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_no_evidence_returns_neutral_reasons_without_model(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên",
            ["missing-a", "missing-b"],
        )

        self.assertEqual(
            [INSUFFICIENT_CONTEXT_REASON, INSUFFICIENT_CONTEXT_REASON],
            [item.reason for item in reasons],
        )
        create_model.assert_not_called()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=None,
    )
    def test_embedding_failure_returns_neutral_reasons_without_retrieval(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        reasons = generate_itinerary_reasons(
            "Tôi thích thiên nhiên",
            ["place-a"],
        )

        self.assertEqual(INSUFFICIENT_CONTEXT_REASON, reasons[0].reason)
        find_chunks.assert_not_called()
        create_model.assert_not_called()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        side_effect=RuntimeError("Gemini unavailable"),
    )
    def test_embedding_exception_is_handled_safely(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        reasons = generate_itinerary_reasons(
            "Preference",
            ["place-a"],
        )

        self.assertEqual(INSUFFICIENT_CONTEXT_REASON, reasons[0].reason)
        find_chunks.assert_not_called()
        create_model.assert_not_called()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_duplicate_input_slugs_remain_deduplicated(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [chunk("place-a", "Context A")]
        model = structured_model(create_model)
        model.invoke.return_value = GeneratedItineraryReasons(
            reasons=[
                GeneratedItineraryReason(
                    place_slug="place-a",
                    reason="Reason A",
                )
            ]
        )

        reasons = generate_itinerary_reasons(
            "Preference",
            ["place-a", "place-a"],
        )

        self.assertEqual(["place-a"], [item.place_slug for item in reasons])
        self.assertEqual(
            ["place-a"],
            find_chunks.call_args.kwargs["place_slugs"],
        )
        embed_query.assert_called_once()

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_rejects_malformed_model_output(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [chunk("place-a", "Context A")]
        structured_model(create_model).invoke.return_value = {
            "unexpected": []
        }

        with self.assertRaisesRegex(
            ValueError,
            "Malformed itinerary explanation output",
        ):
            generate_itinerary_reasons(
                "Tôi thích thiên nhiên",
                ["place-a"],
            )

    @patch("app.rag.itinerary_explanation_service.create_chat_model")
    @patch(
        "app.rag.itinerary_explanation_service."
        "find_relevant_chunks_by_place_slugs"
    )
    @patch(
        "app.rag.itinerary_explanation_service.embed_query",
        return_value=[0.1, 0.2],
    )
    def test_preserves_slug_validation_for_structured_output(
        self,
        embed_query: MagicMock,
        find_chunks: MagicMock,
        create_model: MagicMock,
    ):
        find_chunks.return_value = [
            chunk("place-a", "Context A"),
            chunk("place-b", "Context B"),
        ]
        model = structured_model(create_model)
        cases = [
            (
                "unknown place slug",
                ["place-a"],
                GeneratedItineraryReasons(
                    reasons=[
                        GeneratedItineraryReason(
                            place_slug="place-unknown",
                            reason="Unknown",
                        )
                    ]
                ),
            ),
            (
                "duplicate place slugs",
                ["place-a"],
                GeneratedItineraryReasons(
                    reasons=[
                        GeneratedItineraryReason(
                            place_slug="place-a",
                            reason="First",
                        ),
                        GeneratedItineraryReason(
                            place_slug="place-a",
                            reason="Second",
                        ),
                    ]
                ),
            ),
            (
                "missing a requested place slug",
                ["place-a", "place-b"],
                GeneratedItineraryReasons(
                    reasons=[
                        GeneratedItineraryReason(
                            place_slug="place-a",
                            reason="Only A",
                        )
                    ]
                ),
            ),
            (
                "unknown place slug",
                ["place-a", "place-without-context"],
                GeneratedItineraryReasons(
                    reasons=[
                        GeneratedItineraryReason(
                            place_slug="place-a",
                            reason="Grounded A",
                        ),
                        GeneratedItineraryReason(
                            place_slug="place-without-context",
                            reason="Ungrounded",
                        ),
                    ]
                ),
            ),
        ]

        for expected_message, requested_slugs, response in cases:
            with self.subTest(expected_message=expected_message):
                model.invoke.return_value = response

                with self.assertRaisesRegex(ValueError, expected_message):
                    generate_itinerary_reasons(
                        "Preference",
                        requested_slugs,
                    )


if __name__ == "__main__":
    unittest.main()
