import argparse
import json
from pathlib import Path

from app.db.postgres import pool
from app.recommendation.mmr import select_with_mmr
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
CANDIDATE_K = 30
DEFAULT_MMR_LAMBDAS = (0.7,)


def build_rankings(
    cases: list[RetrievalEvaluationCase],
    mmr_lambdas: list[float],
) -> tuple[
    dict[str, dict[str, list[str]]],
    dict[str, dict[str, list[float]]],
]:
    maximum_top_k = max(TOP_K_VALUES)
    rankings: dict[str, dict[str, list[str]]] = {
        "semantic": {},
        **{
            _mmr_configuration_name(lambda_weight): {}
            for lambda_weight in mmr_lambdas
        },
    }
    embeddings_by_case_id: dict[str, dict[str, list[float]]] = {}

    for case in cases:
        candidates, embeddings_by_slug = retrieve_candidate_places(
            query=case.query,
            fetch_k=FETCH_K,
            candidate_k=CANDIDATE_K,
        )
        embeddings_by_case_id[case.case_id] = embeddings_by_slug

        rankings["semantic"][case.case_id] = [
            candidate.place_slug
            for candidate in candidates[:maximum_top_k]
        ]

        for lambda_weight in mmr_lambdas:
            selected = select_with_mmr(
                candidates=candidates,
                embeddings_by_slug=embeddings_by_slug,
                limit=maximum_top_k,
                lambda_weight=lambda_weight,
            )
            rankings[
                _mmr_configuration_name(lambda_weight)
            ][case.case_id] = [
                candidate.place_slug
                for candidate in selected
            ]

    return rankings, embeddings_by_case_id


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

    for k, diversity in summary.intra_list_diversity_by_k.items():
        print(f"ILD@{k}:     {diversity:.4f}")

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
        description="Evaluate dense retrieval with and without MMR.",
    )
    parser.add_argument(
        "--mmr-lambdas",
        type=_lambda_weight,
        nargs="+",
        default=list(DEFAULT_MMR_LAMBDAS),
        help="MMR lambda values to compare (default: 0.7).",
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
        rankings, embeddings_by_case_id = build_rankings(
            cases,
            args.mmr_lambdas,
        )
    finally:
        pool.close()

    summaries = [
        evaluate_rankings(
            configuration_name=configuration_name,
            cases=cases,
            rankings_by_case_id=configuration_rankings,
            embeddings_by_case_id=embeddings_by_case_id,
            top_k_values=TOP_K_VALUES,
        )
        for configuration_name, configuration_rankings in rankings.items()
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
                "ILD@K": summary.intra_list_diversity_by_k,
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


def _mmr_configuration_name(lambda_weight: float) -> str:
    return f"semantic + MMR (lambda={lambda_weight:g})"


def _lambda_weight(value: str) -> float:
    lambda_weight = float(value)

    if not 0.0 <= lambda_weight <= 1.0:
        raise argparse.ArgumentTypeError(
            "MMR lambda must be between 0 and 1"
        )

    return lambda_weight


if __name__ == "__main__":
    main()
