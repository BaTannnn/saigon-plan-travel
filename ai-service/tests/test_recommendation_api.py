import unittest
from unittest.mock import patch

from app.api.recommendation import (
    PlaceRecommendationRequest,
    recommend_place_candidates,
)


class PlaceRecommendationRequestTest(unittest.TestCase):

    def test_request_contract_contains_only_query_and_top_k(self):
        request = PlaceRecommendationRequest(
            query="history and architecture",
            top_k=30,
        )

        self.assertEqual("history and architecture", request.query)
        self.assertEqual(30, request.top_k)
        self.assertEqual(
            {"query", "top_k"},
            set(PlaceRecommendationRequest.model_fields),
        )

    def test_top_k_keeps_default(self):
        request = PlaceRecommendationRequest(query="history and architecture")

        self.assertEqual(15, request.top_k)

    @patch("app.api.recommendation.recommend_places")
    def test_endpoint_passes_only_query_and_top_k_to_retrieval(
        self,
        recommend_places,
    ):
        recommend_places.return_value = []
        request = PlaceRecommendationRequest(
            query="  history and architecture  ",
            top_k=10,
        )

        response = recommend_place_candidates(request)

        self.assertEqual([], response.candidates)
        recommend_places.assert_called_once_with(
            query="history and architecture",
            top_k=10,
        )


if __name__ == "__main__":
    unittest.main()
