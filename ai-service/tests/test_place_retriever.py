import unittest

from app.knowledge.knowledge_repository import CandidateChunk
from app.recommendation.place_retriever import (
    deduplicate_best_chunk_by_place,
    rank_by_semantic_similarity,
)


def result(
    slug: str,
    section: str,
    similarity: float,
) -> CandidateChunk:
    return CandidateChunk(
        place_slug=slug,
        place_name=slug,
        section=section,
        similarity=similarity,
    )


class PlaceRetrieverTest(unittest.TestCase):

    def test_deduplication_keeps_best_chunk_without_sorting(self):
        chunks = [
            result(
                "buu-dien-trung-tam-sai-gon",
                "OVERVIEW",
                0.88,
            ),
            result(
                "bao-tang-my-thuat-tphcm",
                "OVERVIEW",
                0.89,
            ),
            result(
                "bao-tang-my-thuat-tphcm",
                "HIGHLIGHTS",
                0.94,
            ),
        ]

        candidates = deduplicate_best_chunk_by_place(chunks)

        self.assertEqual(2, len(candidates))
        self.assertEqual(
            ["buu-dien-trung-tam-sai-gon", "bao-tang-my-thuat-tphcm"],
            [candidate.place_slug for candidate in candidates],
        )
        self.assertEqual(
            "HIGHLIGHTS",
            candidates[1].section,
        )
        self.assertEqual(
            0.94,
            candidates[1].similarity,
        )

    def test_semantic_ranking_sorts_descending_without_limiting(self):
        chunks = [
            result("place-b", "OVERVIEW", 0.83),
            result("place-a", "OVERVIEW", 0.91),
            result("place-c", "OVERVIEW", 0.87),
        ]

        candidates = rank_by_semantic_similarity(chunks)

        self.assertEqual(
            ["place-a", "place-c", "place-b"],
            [candidate.place_slug for candidate in candidates],
        )

if __name__ == "__main__":
    unittest.main()
