import unittest

from app.recommendation.mmr import select_with_mmr
from app.recommendation.models import PlaceCandidate


def candidate(
    slug: str,
    score: float,
) -> PlaceCandidate:
    return PlaceCandidate(
        place_slug=slug,
        place_name=slug,
        matched_section="TEST",
        semantic_score=score,
    )


class MmrTest(unittest.TestCase):

    def test_default_lambda_uses_current_production_tradeoff(self):
        candidates = [
            candidate("art-museum", 0.95),
            candidate("art-gallery", 0.93),
            candidate("post-office", 0.80),
        ]

        embeddings = {
            "art-museum": [1.0, 0.0],
            "art-gallery": [0.99, 0.01],
            "post-office": [0.0, 1.0],
        }

        selected = select_with_mmr(
            candidates=candidates,
            embeddings_by_slug=embeddings,
            limit=2,
        )

        self.assertEqual(
            ["art-museum", "art-gallery"],
            [place.place_slug for place in selected],
        )

    def test_prefers_diverse_candidate_over_similar_candidate(self):
        candidates = [
            candidate("art-museum", 0.95),
            candidate("art-gallery", 0.93),
            candidate("post-office", 0.80),
        ]

        embeddings = {
            "art-museum": [1.0, 0.0],
            "art-gallery": [0.99, 0.01],
            "post-office": [0.0, 1.0],
        }

        selected = select_with_mmr(
            candidates=candidates,
            embeddings_by_slug=embeddings,
            limit=2,
            lambda_weight=0.7,
        )

        self.assertEqual(
            ["art-museum", "post-office"],
            [place.place_slug for place in selected],
        )

    def test_lambda_one_behaves_like_relevance_ranking(self):
        candidates = [
            candidate("place-a", 0.95),
            candidate("place-b", 0.90),
            candidate("place-c", 0.80),
        ]

        embeddings = {
            "place-a": [1.0, 0.0],
            "place-b": [1.0, 0.0],
            "place-c": [0.0, 1.0],
        }

        selected = select_with_mmr(
            candidates=candidates,
            embeddings_by_slug=embeddings,
            limit=2,
            lambda_weight=1.0,
        )

        self.assertEqual(
            ["place-a", "place-b"],
            [place.place_slug for place in selected],
        )

if __name__ == "__main__":
    unittest.main()
