import unittest
from unittest.mock import patch

from app.knowledge.knowledge_repository import CandidateChunk
from app.recommendation.models import PlaceCandidate
from app.recommendation.recommendation_service import recommend_places


def chunk(
    slug: str,
    similarity: float,
    section: str = "OVERVIEW",
) -> CandidateChunk:
    return CandidateChunk(
        place_slug=slug,
        place_name=slug,
        section=section,
        similarity=similarity,
    )


class RecommendationServiceTest(unittest.TestCase):

    @patch(
        "app.recommendation.recommendation_service.retrieve_candidate_places"
    )
    def test_requests_only_top_k_candidates(
        self,
        retrieve_candidate_places,
    ):
        retrieve_candidate_places.return_value = []

        candidates = recommend_places(
            query="preferred places",
            top_k=15,
        )

        self.assertEqual([], candidates)
        retrieve_candidate_places.assert_called_once_with(
            query="preferred places",
            top_k=15,
            fetch_k=None,
        )

    @patch("app.recommendation.recommendation_service.search_candidate_chunks")
    @patch("app.recommendation.recommendation_service.embed_query")
    def test_orders_by_best_semantic_score(
        self,
        embed_query,
        search_candidate_chunks,
    ):
        embed_query.return_value = [1.0, 0.0]
        search_candidate_chunks.return_value = [
            chunk("place-a", 0.91),
            chunk("place-b", 0.83),
            chunk("place-c", 0.87),
        ]

        candidates = recommend_places(
            query="preferred places",
            top_k=3,
        )

        self.assertEqual(
            ["place-a", "place-c", "place-b"],
            [candidate.place_slug for candidate in candidates],
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
            chunk("place-a", 0.90 - index / 1000)
            for index in range(50)
        ]
        search_candidate_chunks.side_effect = [
            duplicate_chunks,
            [
                *duplicate_chunks,
                chunk("place-b", 0.80),
            ],
        ]

        candidates = recommend_places(
            query="preferred places",
            top_k=2,
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

    @patch(
        "app.recommendation.recommendation_service.retrieve_candidate_places"
    )
    def test_top_k_is_respected(
        self,
        retrieve_candidate_places,
    ):
        retrieve_candidate_places.return_value = [
            PlaceCandidate("place-a", "Place A", "OVERVIEW", 0.91),
            PlaceCandidate("place-b", "Place B", "OVERVIEW", 0.83),
        ]

        candidates = recommend_places(query="preferred places", top_k=1)

        self.assertEqual(["place-a"], [item.place_slug for item in candidates])


if __name__ == "__main__":
    unittest.main()
