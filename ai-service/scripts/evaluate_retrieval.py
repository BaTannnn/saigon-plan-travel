import argparse
import json
from pathlib import Path

from app.db.postgres import pool
from app.recommendation.recommendation_service import (
    retrieve_candidate_places,
)
from app.recommendation.retrieval_evaluation import (
    EvaluationSummary,
    RetrievalEvaluationCase,
    evaluate_rankings,
    load_evaluation_cases,
)


DATASET_PATH = (
    Path(__file__).resolve().parents[1]
    / "tests"
    / "retrieval_cases.json"
)
CORPUS_DIR = Path(__file__).resolve().parents[1] / "corpus"
TOP_K_VALUES = (5, 10, 15)
FETCH_K = 50


def build_rankings(
    cases: list[RetrievalEvaluationCase],
) -> dict[str, list[str]]:
    maximum_top_k = max(TOP_K_VALUES)
    rankings: dict[str, list[str]] = {}

    for case in cases:
        candidates = retrieve_candidate_places(
            query=case.query,
            top_k=maximum_top_k,
            fetch_k=FETCH_K,
        )
        rankings[case.case_id] = [
            candidate.place_slug
            for candidate in candidates[:maximum_top_k]
        ]

    return rankings


def print_summary(
    summary: EvaluationSummary,
    *,
    show_cases: bool,
) -> None:
    print()
    print(f"Configuration: {summary.configuration_name}")

    for k, recall in summary.recall_by_k.items():
        print(f"Recall@{k}:  {recall:.4f}")

    print(f"MRR:        {summary.mean_reciprocal_rank:.4f}")

    if not show_cases:
        return

    print("Queries:")

    for query_evaluation in summary.query_evaluations:
        print(f"  [{query_evaluation.case.case_id}]")
        print(
            "    Expected: "
            f"{query_evaluation.case.relevant_place_slugs}"
        )
        print(
            "    Retrieved: "
            f"{query_evaluation.retrieved_place_slugs}"
        )
        print(f"    RR: {query_evaluation.reciprocal_rank:.4f}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Evaluate semantic retrieval.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="Optional path for a machine-readable JSON result.",
    )
    parser.add_argument(
        "--show-cases",
        action="store_true",
        help="Show concise per-case rankings in terminal output.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    cases = load_evaluation_cases(
        DATASET_PATH,
        CORPUS_DIR,
    )

    print("RAG Retrieval Evaluation")
    print("=" * 40)
    print(f"Cases: {len(cases)}")

    pool.open()

    try:
        rankings = build_rankings(cases)
    finally:
        pool.close()

    summaries = [
        evaluate_rankings(
            configuration_name="semantic",
            cases=cases,
            rankings_by_case_id=rankings,
            top_k_values=TOP_K_VALUES,
        )
    ]

    for summary in summaries:
        print_summary(summary, show_cases=args.show_cases)

    if args.output is not None:
        write_json_result(
            output_path=args.output,
            summaries=summaries,
        )


def write_json_result(
    *,
    output_path: Path,
    summaries: list[EvaluationSummary],
) -> None:
    result = {
        "case_count": len(summaries[0].query_evaluations),
        "configurations": [
            {
                "configuration": summary.configuration_name,
                "Recall@K": summary.recall_by_k,
                "MRR": summary.mean_reciprocal_rank,
                "cases": [
                    {
                        "id": query_evaluation.case.case_id,
                        "retrievedPlaceSlugs": (
                            query_evaluation.retrieved_place_slugs
                        ),
                    }
                    for query_evaluation in summary.query_evaluations
                ],
            }
            for summary in summaries
        ],
    }

    output_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print()
    print(f"Wrote JSON result: {output_path}")


if __name__ == "__main__":
    main()
