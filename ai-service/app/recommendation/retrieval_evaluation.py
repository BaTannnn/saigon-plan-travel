import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class RetrievalEvaluationCase:
    case_id: str
    query: str
    relevant_place_slugs: list[str]


@dataclass(frozen=True)
class QueryEvaluation:
    case: RetrievalEvaluationCase
    retrieved_place_slugs: list[str]
    recall_by_k: dict[int, float]
    reciprocal_rank: float


@dataclass(frozen=True)
class EvaluationSummary:
    configuration_name: str
    query_evaluations: list[QueryEvaluation]
    recall_by_k: dict[int, float]
    mean_reciprocal_rank: float


def recall_at_k(
    relevant_place_slugs: list[str],
    retrieved_place_slugs: list[str],
    k: int,
) -> float:
    relevant = _validated_relevant_places(relevant_place_slugs)

    if k <= 0:
        raise ValueError("k must be greater than zero")

    retrieved = set(retrieved_place_slugs[:k])
    matches = relevant.intersection(retrieved)

    return len(matches) / len(relevant)


def reciprocal_rank(
    relevant_place_slugs: list[str],
    retrieved_place_slugs: list[str],
) -> float:
    relevant = _validated_relevant_places(relevant_place_slugs)

    for rank, place_slug in enumerate(
        retrieved_place_slugs,
        start=1,
    ):
        if place_slug in relevant:
            return 1.0 / rank

    return 0.0


def mean_reciprocal_rank(
    rankings: list[tuple[list[str], list[str]]],
) -> float:
    if not rankings:
        raise ValueError("At least one evaluation ranking is required")

    return sum(
        reciprocal_rank(relevant, retrieved)
        for relevant, retrieved in rankings
    ) / len(rankings)


def load_evaluation_cases(
    dataset_path: Path,
    corpus_dir: Path,
) -> list[RetrievalEvaluationCase]:
    raw_cases = json.loads(dataset_path.read_text(encoding="utf-8"))

    if not isinstance(raw_cases, list) or not raw_cases:
        raise ValueError("Evaluation dataset must be a non-empty JSON list")

    cases: list[RetrievalEvaluationCase] = []
    seen_ids: set[str] = set()
    corpus_place_slugs = _load_corpus_place_slugs(corpus_dir)

    for raw_case in raw_cases:
        if not isinstance(raw_case, dict):
            raise ValueError("Each evaluation case must be a JSON object")

        case_id = raw_case.get("id")
        query = raw_case.get("query")
        relevant_place_slugs = raw_case.get("expectedPlaceSlugs")

        if not isinstance(case_id, str) or not case_id.strip():
            raise ValueError("Each evaluation case must have a non-empty id")

        if case_id in seen_ids:
            raise ValueError(f"Duplicate evaluation case id: {case_id}")

        if not isinstance(query, str) or not query.strip():
            raise ValueError(
                f"Evaluation case {case_id} must have a non-empty query"
            )

        if not isinstance(relevant_place_slugs, list):
            raise ValueError(
                f"Evaluation case {case_id} must have expectedPlaceSlugs"
            )

        _validated_relevant_places(relevant_place_slugs)

        unknown_place_slugs = (
            set(relevant_place_slugs) - corpus_place_slugs
        )

        if unknown_place_slugs:
            unknown = ", ".join(sorted(unknown_place_slugs))
            raise ValueError(
                f"Evaluation case {case_id} has unknown place slugs: "
                f"{unknown}"
            )

        cases.append(
            RetrievalEvaluationCase(
                case_id=case_id,
                query=query,
                relevant_place_slugs=relevant_place_slugs,
            )
        )
        seen_ids.add(case_id)

    return cases


def evaluate_rankings(
    *,
    configuration_name: str,
    cases: list[RetrievalEvaluationCase],
    rankings_by_case_id: dict[str, list[str]],
    top_k_values: tuple[int, ...],
) -> EvaluationSummary:
    if not cases:
        raise ValueError("At least one evaluation case is required")

    if not top_k_values:
        raise ValueError("At least one Top-K value is required")

    query_evaluations: list[QueryEvaluation] = []

    for case in cases:
        if case.case_id not in rankings_by_case_id:
            raise ValueError(
                f"Missing retrieval ranking for case: {case.case_id}"
            )

        retrieved = rankings_by_case_id[case.case_id]
        recalls = {
            k: recall_at_k(
                case.relevant_place_slugs,
                retrieved,
                k,
            )
            for k in top_k_values
        }
        query_evaluations.append(
            QueryEvaluation(
                case=case,
                retrieved_place_slugs=retrieved,
                recall_by_k=recalls,
                reciprocal_rank=reciprocal_rank(
                    case.relevant_place_slugs,
                    retrieved,
                ),
            )
        )

    average_recall_by_k = {
        k: sum(
            query_evaluation.recall_by_k[k]
            for query_evaluation in query_evaluations
        )
        / len(query_evaluations)
        for k in top_k_values
    }

    mrr = mean_reciprocal_rank(
        [
            (
                query_evaluation.case.relevant_place_slugs,
                query_evaluation.retrieved_place_slugs,
            )
            for query_evaluation in query_evaluations
        ]
    )

    return EvaluationSummary(
        configuration_name=configuration_name,
        query_evaluations=query_evaluations,
        recall_by_k=average_recall_by_k,
        mean_reciprocal_rank=mrr,
    )


def _load_corpus_place_slugs(corpus_dir: Path) -> set[str]:
    corpus_files = sorted(corpus_dir.glob("*.json"))

    if not corpus_files:
        raise ValueError("Corpus directory must contain JSON files")

    place_slugs: set[str] = set()

    for corpus_path in corpus_files:
        raw_corpus = json.loads(
            corpus_path.read_text(encoding="utf-8")
        )
        place_slug = raw_corpus.get("placeSlug")

        if not isinstance(place_slug, str) or not place_slug.strip():
            raise ValueError(
                f"Corpus file has no valid placeSlug: {corpus_path}"
            )

        place_slugs.add(place_slug)

    return place_slugs


def _validated_relevant_places(
    relevant_place_slugs: list[str],
) -> set[str]:
    if not relevant_place_slugs:
        raise ValueError("Relevant place slugs must not be empty")

    if not all(
        isinstance(place_slug, str) and place_slug.strip()
        for place_slug in relevant_place_slugs
    ):
        raise ValueError("Relevant place slugs must be non-empty strings")

    relevant = set(relevant_place_slugs)

    if len(relevant) != len(relevant_place_slugs):
        raise ValueError("Relevant place slugs must not contain duplicates")

    return relevant
