import unittest
from unittest.mock import patch

from app.rag.knowledge_repository import CandidateChunk
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
    def test_deduplicates_eligible_chunks_before_mmr_with_production_lambda(
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


if __name__ == "__main__":
    unittest.main()
