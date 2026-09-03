import os

from dotenv import load_dotenv

DEFAULT_RETRIEVAL_FETCH_K = 50

load_dotenv()


def get_retrieval_fetch_k(override: int | None = None) -> int:
    return _positive_int_setting(
        name="RETRIEVAL_FETCH_K",
        default=DEFAULT_RETRIEVAL_FETCH_K,
        override=override,
    )


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
