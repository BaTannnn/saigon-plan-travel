import unittest
from unittest.mock import patch

from pydantic import ValidationError

from app.api.recommendation import (
    PlaceRecommendationRequest,
    recommend_place_candidates,
)


class PlaceRecommendationRequestTest(unittest.TestCase):

    def test_accepts_empty_whitelist(self):
        request = PlaceRecommendationRequest(
            query="history and architecture",
            eligible_place_slugs=[],
        )

        self.assertEqual([], request.eligible_place_slugs)

    def test_normalizes_duplicate_and_padded_slugs(self):
        request = PlaceRecommendationRequest(
            query="history and architecture",
            eligible_place_slugs=[
                " dinh-doc-lap ",
                "dinh-doc-lap",
                "buu-dien-trung-tam-sai-gon",
            ],
        )

        self.assertEqual(
            ["dinh-doc-lap", "buu-dien-trung-tam-sai-gon"],
            request.eligible_place_slugs,
        )

    def test_rejects_non_string_slug(self):
        with self.assertRaises(ValidationError):
            PlaceRecommendationRequest(
                query="history and architecture",
                eligible_place_slugs=[123],
            )

    def test_rejects_missing_whitelist(self):
        with self.assertRaises(ValidationError):
            PlaceRecommendationRequest(query="history and architecture")

    @patch("app.api.recommendation.recommend_places")
    def test_endpoint_passes_explicit_whitelist_to_retrieval(
        self,
        recommend_places,
    ):
        recommend_places.return_value = []
        request = PlaceRecommendationRequest(
            query="  history and architecture  ",
            top_k=10,
            eligible_place_slugs=["dinh-doc-lap"],
        )

        response = recommend_place_candidates(request)

        self.assertEqual([], response.candidates)
        recommend_places.assert_called_once_with(
            query="history and architecture",
            top_k=10,
            eligible_place_slugs=["dinh-doc-lap"],
        )


if __name__ == "__main__":
    unittest.main()
