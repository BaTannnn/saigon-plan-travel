import os
import unittest
from unittest.mock import patch

from app.knowledge.knowledge_repository import CandidateChunk
from app.recommendation.recommendation_service import recommend_places


def chunk(
    slug: str,
    similarity: float,
    embedding: list[float],
    section: str = "OVERVIEW",
) -> CandidateChunk:
    return CandidateChunk(
        place_slug=slug,
        place_name=slug,
        section=section,
        similarity=similarity,
        embedding=embedding,
    )


class RecommendationServiceTest(unittest.TestCase):

    @patch(
        "app.recommendation.recommendation_service.retrieve_candidate_places"
    )
    def test_semantic_runtime_requests_only_top_k_candidates(
        self,
        retrieve_candidate_places,
    ):
        retrieve_candidate_places.return_value = ([], {})

        candidates = recommend_places(
            query="preferred places",
            top_k=15,
            candidate_k=30,
            retrieval_mode="semantic",
        )

        self.assertEqual([], candidates)
        retrieve_candidate_places.assert_called_once_with(
            query="preferred places",
            fetch_k=None,
            candidate_k=15,
            eligible_place_slugs=None,
        )

    @patch("app.recommendation.recommendation_service.select_with_mmr")
    @patch(
        "app.recommendation.recommendation_service.retrieve_candidate_places"
    )
    def test_explicit_mmr_requests_larger_candidate_pool(
        self,
        retrieve_candidate_places,
        select_with_mmr,
    ):
        retrieve_candidate_places.return_value = ([], {})
        select_with_mmr.return_value = []

        candidates = recommend_places(
            query="preferred places",
            top_k=15,
            candidate_k=30,
            retrieval_mode="mmr",
        )

        self.assertEqual([], candidates)
        retrieve_candidate_places.assert_called_once_with(
            query="preferred places",
            fetch_k=None,
            candidate_k=30,
            eligible_place_slugs=None,
        )

    @patch("app.recommendation.recommendation_service.select_with_mmr")
    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_semantic_is_runtime_default_and_orders_by_best_score(
        self,
        embed_query,
        search_candidate_chunks,
        select_with_mmr,
    ):
        embed_query.return_value = [1.0, 0.0]
        search_candidate_chunks.return_value = [
            chunk("place-a", 0.91, [1.0, 0.0]),
            chunk("place-b", 0.83, [0.8, 0.2]),
            chunk("place-c", 0.87, [0.9, 0.1]),
        ]

        with patch.dict(os.environ, {}, clear=True):
            candidates = recommend_places(
                query="preferred places",
                top_k=3,
            )

        self.assertEqual(
            ["place-a", "place-c", "place-b"],
            [candidate.place_slug for candidate in candidates],
        )
        select_with_mmr.assert_not_called()

    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_retrieves_inside_whitelist_to_prevent_candidate_starvation(
        self,
        embed_query,
        search_candidate_chunks,
    ):
        eligible_slugs = ["place-d", "place-e", "place-f", "place-g", "place-h"]
        embed_query.return_value = [1.0, 0.0]
        search_candidate_chunks.return_value = [
            chunk("place-d", 0.95, [1.0, 0.0]),
            chunk("place-e", 0.94, [0.99, 0.01]),
            chunk("place-f", 0.93, [0.98, 0.02]),
            chunk("place-g", 0.92, [0.97, 0.03]),
            chunk("place-h", 0.91, [0.96, 0.04]),
        ]

        candidates = recommend_places(
            query="preferred places",
            top_k=5,
            eligible_place_slugs=eligible_slugs,
        )

        self.assertEqual(
            eligible_slugs,
            [candidate.place_slug for candidate in candidates],
        )
        search_candidate_chunks.assert_called_once_with(
            query_embedding=[1.0, 0.0],
            limit=50,
            eligible_place_slugs=eligible_slugs,
        )

    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_empty_whitelist_returns_empty_without_embedding_or_database(
        self,
        embed_query,
        search_candidate_chunks,
    ):
        candidates = recommend_places(
            query="preferred places",
            eligible_place_slugs=[],
        )

        self.assertEqual([], candidates)
        embed_query.assert_not_called()
        search_candidate_chunks.assert_not_called()

    @patch("app.recommendation.recommendation_service.select_with_mmr")
    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_explicit_mmr_deduplicates_chunks_and_uses_configured_lambda(
        self,
        embed_query,
        search_candidate_chunks,
        select_with_mmr,
    ):
        embed_query.return_value = [1.0, 0.0]
        search_candidate_chunks.return_value = [
            chunk("place-d", 0.95, [1.0, 0.0], "OVERVIEW"),
            chunk("place-d", 0.90, [0.9, 0.1], "HIGHLIGHTS"),
            chunk("place-e", 0.85, [0.0, 1.0]),
        ]
        select_with_mmr.side_effect = lambda candidates, **kwargs: candidates

        candidates = recommend_places(
            query="preferred places",
            retrieval_mode="mmr",
            eligible_place_slugs=["place-d", "place-e"],
        )

        self.assertEqual(
            ["place-d", "place-e"],
            [candidate.place_slug for candidate in candidates],
        )
        mmr_arguments = select_with_mmr.call_args.kwargs
        self.assertEqual(0.9, mmr_arguments["lambda_weight"])
        self.assertEqual(
            ["place-d", "place-e"],
            [candidate.place_slug for candidate in mmr_arguments["candidates"]],
        )

    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_fetch_expands_until_top_k_unique_places_are_available(
        self,
        embed_query,
        search_candidate_chunks,
    ):
        embed_query.return_value = [1.0, 0.0]
        duplicate_chunks = [
            chunk("place-a", 0.90 - index / 1000, [1.0, 0.0])
            for index in range(50)
        ]
        search_candidate_chunks.side_effect = [
            duplicate_chunks,
            [
                *duplicate_chunks,
                chunk("place-b", 0.80, [0.0, 1.0]),
            ],
        ]

        candidates = recommend_places(
            query="preferred places",
            top_k=2,
            candidate_k=2,
        )

        self.assertEqual(
            ["place-a", "place-b"],
            [candidate.place_slug for candidate in candidates],
        )
        self.assertEqual(
            [50, 100],
            [
                call.kwargs["limit"]
                for call in search_candidate_chunks.call_args_list
            ],
        )

    @patch("app.recommendation.recommendation_service.select_with_mmr")
    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_semantic_mode_does_not_read_mmr_lambda(
        self,
        embed_query,
        search_candidate_chunks,
        select_with_mmr,
    ):
        embed_query.return_value = [1.0, 0.0]
        search_candidate_chunks.return_value = [
            chunk("place-a", 0.91, [1.0, 0.0]),
        ]

        with patch.dict(
            os.environ,
            {"RETRIEVAL_MODE": "semantic", "MMR_LAMBDA": "not-a-number"},
        ):
            candidates = recommend_places(query="preferred places")

        self.assertEqual(["place-a"], [item.place_slug for item in candidates])
        select_with_mmr.assert_not_called()


if __name__ == "__main__":
    unittest.main()
