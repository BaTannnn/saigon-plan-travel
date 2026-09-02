import os

from dotenv import load_dotenv

DEFAULT_RETRIEVAL_MODE = "semantic"
DEFAULT_RETRIEVAL_FETCH_K = 50
DEFAULT_RETRIEVAL_CANDIDATE_K = 30
DEFAULT_MMR_LAMBDA = 0.9
RETRIEVAL_MODES = {"semantic", "mmr"}

load_dotenv()


def get_retrieval_mode(override: str | None = None) -> str:
    mode = (
        override
        if override is not None
        else os.getenv("RETRIEVAL_MODE", DEFAULT_RETRIEVAL_MODE)
    ).strip().lower()

    if mode not in RETRIEVAL_MODES:
        allowed = ", ".join(sorted(RETRIEVAL_MODES))
        raise ValueError(f"RETRIEVAL_MODE must be one of: {allowed}")

    return mode


def get_retrieval_fetch_k(override: int | None = None) -> int:
    return _positive_int_setting(
        name="RETRIEVAL_FETCH_K",
        default=DEFAULT_RETRIEVAL_FETCH_K,
        override=override,
    )


def get_retrieval_candidate_k(override: int | None = None) -> int:
    return _positive_int_setting(
        name="RETRIEVAL_CANDIDATE_K",
        default=DEFAULT_RETRIEVAL_CANDIDATE_K,
        override=override,
    )


def get_mmr_lambda(override: float | None = None) -> float:
    value = (
        override
        if override is not None
        else float(os.getenv("MMR_LAMBDA", str(DEFAULT_MMR_LAMBDA)))
    )

    if not 0.0 <= value <= 1.0:
        raise ValueError("MMR_LAMBDA must be between 0 and 1")

    return value


def _positive_int_setting(
    *,
    name: str,
    default: int,
    override: int | None,
) -> int:
    value = override if override is not None else int(os.getenv(name, default))

    if value <= 0:
        raise ValueError(f"{name} must be greater than zero")

    return value
