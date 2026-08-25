import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from app.recommendation.retrieval_evaluation import (
    RetrievalEvaluationCase,
    evaluate_rankings,
    intra_list_diversity_at_k,
    load_evaluation_cases,
    mean_reciprocal_rank,
    recall_at_k,
    reciprocal_rank,
)


class RetrievalMetricTest(unittest.TestCase):

    def test_recall_and_rr_for_multiple_relevant_results(self):
        relevant = ["a", "b"]
        retrieved = ["x", "a", "y", "b", "z"]

        self.assertEqual(0.0, recall_at_k(relevant, retrieved, 1))
        self.assertEqual(0.5, recall_at_k(relevant, retrieved, 3))
        self.assertEqual(1.0, recall_at_k(relevant, retrieved, 5))
        self.assertEqual(0.5, reciprocal_rank(relevant, retrieved))

    def test_no_relevant_result(self):
        relevant = ["a", "b"]
        retrieved = ["x", "y", "z"]

        self.assertEqual(0.0, recall_at_k(relevant, retrieved, 3))
        self.assertEqual(0.0, reciprocal_rank(relevant, retrieved))

    def test_first_result_relevant(self):
        relevant = ["a"]
        retrieved = ["a", "x"]

        self.assertEqual(1.0, recall_at_k(relevant, retrieved, 1))
        self.assertEqual(1.0, reciprocal_rank(relevant, retrieved))

    def test_empty_relevant_list_fails_clearly(self):
        with self.assertRaisesRegex(ValueError, "must not be empty"):
            recall_at_k([], ["a"], 5)

        with self.assertRaisesRegex(ValueError, "must not be empty"):
            reciprocal_rank([], ["a"])

    def test_mean_reciprocal_rank(self):
        rankings = [
            (["a"], ["a", "x"]),
            (["b"], ["x", "b"]),
            (["c"], ["x", "y"]),
        ]

        self.assertAlmostEqual(
            0.5,
            mean_reciprocal_rank(rankings),
        )

    def test_ild_averages_pairwise_dissimilarity(self):
        retrieved = ["a", "b", "c"]
        embeddings = {
            "a": [1.0, 0.0],
            "b": [0.0, 1.0],
            "c": [1.0, 0.0],
        }

        self.assertAlmostEqual(
            2.0 / 3.0,
            intra_list_diversity_at_k(
                retrieved,
                embeddings,
                3,
            ),
        )

    def test_ild_is_zero_for_fewer_than_two_items(self):
        self.assertEqual(
            0.0,
            intra_list_diversity_at_k([], {}, 5),
        )
        self.assertEqual(
            0.0,
            intra_list_diversity_at_k(
                ["a"],
                {"a": [1.0, 0.0]},
                5,
            ),
        )

    def test_ild_fails_when_embedding_is_missing(self):
        with self.assertRaisesRegex(
            ValueError,
            "Missing embedding for place: b",
        ):
            intra_list_diversity_at_k(
                ["a", "b"],
                {"a": [1.0, 0.0]},
                5,
            )

    def test_evaluation_summary_averages_each_query(self):
        cases = [
            RetrievalEvaluationCase("case-a", "query a", ["a"]),
            RetrievalEvaluationCase("case-b", "query b", ["b"]),
        ]

        summary = evaluate_rankings(
            configuration_name="semantic",
            cases=cases,
            rankings_by_case_id={
                "case-a": ["a", "x"],
                "case-b": ["x", "b"],
            },
            embeddings_by_case_id={
                "case-a": {
                    "a": [1.0, 0.0],
                    "x": [0.0, 1.0],
                },
                "case-b": {
                    "x": [1.0, 0.0],
                    "b": [0.0, 1.0],
                },
            },
            top_k_values=(1, 2),
        )

        self.assertEqual({1: 0.5, 2: 1.0}, summary.recall_by_k)
        self.assertEqual(0.75, summary.mean_reciprocal_rank)
        self.assertEqual(
            {1: 0.0, 2: 1.0},
            summary.intra_list_diversity_by_k,
        )


class RetrievalDatasetValidationTest(unittest.TestCase):

    def setUp(self):
        self.temporary_directory = TemporaryDirectory()
        temporary_path = Path(self.temporary_directory.name)
        self.dataset_path = temporary_path / "cases.json"
        self.corpus_dir = temporary_path / "corpus"
        self.corpus_dir.mkdir()
        (self.corpus_dir / "place-a.json").write_text(
            json.dumps({"placeSlug": "place-a"}),
            encoding="utf-8",
        )

    def tearDown(self):
        self.temporary_directory.cleanup()

    def test_loads_valid_dataset(self):
        self._write_cases(
            [
                {
                    "id": "case-01",
                    "query": "A real travel preference",
                    "expectedPlaceSlugs": ["place-a"],
                }
            ]
        )

        cases = load_evaluation_cases(
            self.dataset_path,
            self.corpus_dir,
        )

        self.assertEqual(["case-01"], [case.case_id for case in cases])

    def test_rejects_duplicate_case_ids(self):
        self._write_cases(
            [
                self._case(case_id="duplicate"),
                self._case(case_id="duplicate"),
            ]
        )

        with self.assertRaisesRegex(ValueError, "Duplicate.*duplicate"):
            self._load_cases()

    def test_rejects_empty_query(self):
        self._write_cases([self._case(query="  ")])

        with self.assertRaisesRegex(ValueError, "non-empty query"):
            self._load_cases()

    def test_rejects_empty_relevant_slugs(self):
        self._write_cases([self._case(relevant=[])])

        with self.assertRaisesRegex(ValueError, "must not be empty"):
            self._load_cases()

    def test_rejects_duplicate_relevant_slugs(self):
        self._write_cases(
            [self._case(relevant=["place-a", "place-a"])]
        )

        with self.assertRaisesRegex(ValueError, "must not contain duplicates"):
            self._load_cases()

    def test_rejects_slug_missing_from_corpus(self):
        self._write_cases([self._case(relevant=["missing-place"])])

        with self.assertRaisesRegex(ValueError, "unknown place slugs"):
            self._load_cases()

    def _load_cases(self):
        return load_evaluation_cases(
            self.dataset_path,
            self.corpus_dir,
        )

    def _write_cases(self, cases: list[dict]) -> None:
        self.dataset_path.write_text(
            json.dumps(cases),
            encoding="utf-8",
        )

    @staticmethod
    def _case(
        *,
        case_id: str = "case-01",
        query: str = "A real travel preference",
        relevant: list[str] | None = None,
    ) -> dict:
        return {
            "id": case_id,
            "query": query,
            "expectedPlaceSlugs": (
                ["place-a"] if relevant is None else relevant
            ),
        }


if __name__ == "__main__":
    unittest.main()
