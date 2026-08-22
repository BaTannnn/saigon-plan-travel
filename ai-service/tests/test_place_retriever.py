import unittest

from app.recommendation.place_retriever import select_unique_candidate_chunks


from app.rag.knowledge_repository import CandidateChunk


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
        embedding=[1.0, 0.0],
    )


class PlaceRetrieverTest(unittest.TestCase):

    def test_keeps_only_best_chunk_for_each_place(self):
        results = [
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
            result(
                "buu-dien-trung-tam-sai-gon",
                "OVERVIEW",
                0.88,
            ),
        ]

        candidates = select_unique_candidate_chunks(
            results=results,
            limit=10,
        )

        self.assertEqual(2, len(candidates))

        self.assertEqual(
            "bao-tang-my-thuat-tphcm",
            candidates[0].place_slug,
        )

        self.assertEqual(
            "HIGHLIGHTS",
            candidates[0].section,
        )

        self.assertEqual(
            0.94,
            candidates[0].similarity,
        )

    def test_limits_number_of_unique_places(self):
        results = [
            result("place-a", "OVERVIEW", 0.90),
            result("place-b", "OVERVIEW", 0.80),
            result("place-c", "OVERVIEW", 0.70),
        ]

        candidates = select_unique_candidate_chunks(
            results=results,
            limit=2,
        )

        self.assertEqual(2, len(candidates))

        self.assertEqual(
            ["place-a", "place-b"],
            [candidate.place_slug for candidate in candidates],
        )

if __name__ == "__main__":
    unittest.main()
