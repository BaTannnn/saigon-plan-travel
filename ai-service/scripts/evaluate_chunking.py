import argparse
from dataclasses import dataclass
from pathlib import Path
from statistics import mean

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.knowledge.chunking import build_place_markdown, split_place_corpus
from app.knowledge.corpus_schema import PlaceCorpus


PROJECT_DIR = Path(__file__).resolve().parents[1]
CORPUS_DIR = PROJECT_DIR / "corpus"
DEFAULT_OUTPUT = Path("chunking-samples.md")

SAMPLE_PLACE_SLUGS = (
    "dinh-doc-lap",
    "buu-dien-trung-tam-sai-gon",
    "bao-tang-my-thuat-tphcm",
    "cho-ben-thanh",
    "thao-cam-vien-sai-gon",
    "chua-ba-thien-hau",
)

RECURSIVE_500 = "Recursive 500/50"
RECURSIVE_800 = "Recursive 800/100"
MARKDOWN_HEADER = "MarkdownHeader + Recursive fallback"
CONFIGURATION_ORDER = (
    RECURSIVE_500,
    RECURSIVE_800,
    MARKDOWN_HEADER,
)
CONFIGURATION_HEADINGS = {
    RECURSIVE_500: "Configuration A — Recursive 500/50",
    RECURSIVE_800: "Configuration B — Recursive 800/100",
    MARKDOWN_HEADER: (
        "Configuration C — MarkdownHeader + Recursive 800/100"
    ),
}

RECURSIVE_500_CHUNK_SIZE = 500
RECURSIVE_500_CHUNK_OVERLAP = 50
RECURSIVE_800_CHUNK_SIZE = 800
RECURSIVE_800_CHUNK_OVERLAP = 100


@dataclass(frozen=True)
class SampleChunk:
    content: str
    section: str | None = None


@dataclass(frozen=True)
class ChunkStatistics:
    chunk_count: int
    minimum_length: int
    average_length: float
    maximum_length: int


def load_sample_corpora(
    corpus_dir: Path = CORPUS_DIR,
    sample_place_slugs: tuple[str, ...] = SAMPLE_PLACE_SLUGS,
) -> list[PlaceCorpus]:
    corpora_by_slug: dict[str, PlaceCorpus] = {}

    for path in sorted(corpus_dir.glob("*.json")):
        corpus = PlaceCorpus.model_validate_json(
            path.read_text(encoding="utf-8")
        )
        corpora_by_slug[corpus.placeSlug] = corpus

    missing = [
        slug for slug in sample_place_slugs if slug not in corpora_by_slug
    ]
    if missing:
        raise ValueError(
            "Sample places are missing from the corpus: "
            + ", ".join(missing)
        )

    return [corpora_by_slug[slug] for slug in sample_place_slugs]


def split_recursive_text(
    text: str,
    *,
    chunk_size: int,
    chunk_overlap: int,
) -> list[SampleChunk]:
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        length_function=len,
    )
    chunks = [
        SampleChunk(content=content)
        for content in splitter.split_text(text)
    ]
    _ensure_non_empty_chunks(chunks)
    return chunks


def build_configurations(
    corpus: PlaceCorpus,
) -> dict[str, list[SampleChunk]]:
    markdown = build_place_markdown(corpus).strip()
    production_chunks = [
        SampleChunk(
            content=chunk.content,
            section=chunk.section,
        )
        for chunk in split_place_corpus(corpus)
    ]
    _ensure_non_empty_chunks(production_chunks)

    return {
        RECURSIVE_500: split_recursive_text(
            markdown,
            chunk_size=RECURSIVE_500_CHUNK_SIZE,
            chunk_overlap=RECURSIVE_500_CHUNK_OVERLAP,
        ),
        RECURSIVE_800: split_recursive_text(
            markdown,
            chunk_size=RECURSIVE_800_CHUNK_SIZE,
            chunk_overlap=RECURSIVE_800_CHUNK_OVERLAP,
        ),
        MARKDOWN_HEADER: production_chunks,
    }


def calculate_statistics(
    chunks: list[SampleChunk],
) -> ChunkStatistics:
    _ensure_non_empty_chunks(chunks)
    lengths = [len(chunk.content) for chunk in chunks]

    return ChunkStatistics(
        chunk_count=len(chunks),
        minimum_length=min(lengths),
        average_length=mean(lengths),
        maximum_length=max(lengths),
    )


def build_markdown_report(
    corpora: list[PlaceCorpus],
) -> tuple[str, dict[str, ChunkStatistics]]:
    if not corpora:
        raise ValueError("At least one sample Place corpus is required")

    lines = _report_introduction(corpora)
    all_chunks = {
        configuration: []
        for configuration in CONFIGURATION_ORDER
    }

    for corpus in corpora:
        lines.extend([f"## Place: {corpus.placeSlug}", ""])
        configurations = build_configurations(corpus)

        for configuration in CONFIGURATION_ORDER:
            chunks = configurations[configuration]
            statistics = calculate_statistics(chunks)
            all_chunks[configuration].extend(chunks)
            lines.extend(
                _configuration_report(
                    configuration=configuration,
                    chunks=chunks,
                    statistics=statistics,
                )
            )

    summary = {
        configuration: calculate_statistics(chunks)
        for configuration, chunks in all_chunks.items()
    }
    lines.extend(_summary_report(summary))
    return "\n".join(lines).rstrip() + "\n", summary


def write_markdown_report(
    *,
    output_path: Path,
    report: str,
) -> None:
    output_path.write_text(report, encoding="utf-8")


def print_summary(
    *,
    sample_count: int,
    summary: dict[str, ChunkStatistics],
    output_path: Path,
) -> None:
    print("Chunking Qualitative Experiment")
    print("=" * 64)
    print(f"Sample places: {sample_count}")
    print()
    print(f"{'Configuration':<38} {'Chunks':>8} {'Avg length':>12}")

    for configuration in CONFIGURATION_ORDER:
        statistics = summary[configuration]
        print(
            f"{configuration:<38} "
            f"{statistics.chunk_count:>8} "
            f"{statistics.average_length:>12.1f}"
        )

    print()
    print(f"Wrote Markdown report: {output_path}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate an offline qualitative report for chunking samples."
        )
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Markdown report path (default: chunking-samples.md).",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    corpora = load_sample_corpora()
    report, summary = build_markdown_report(corpora)
    write_markdown_report(output_path=args.output, report=report)
    print_summary(
        sample_count=len(corpora),
        summary=summary,
        output_path=args.output,
    )


def _report_introduction(corpora: list[PlaceCorpus]) -> list[str]:
    sample_lines = [
        f"- `{corpus.placeSlug}`"
        for corpus in corpora
    ]
    return [
        "# Chunking Qualitative Experiment",
        "",
        "This offline experiment compares three practical chunking "
        "configurations on representative entries from the existing JSON "
        "corpus. Lengths are measured in characters with Python `len()`.",
        "",
        "Current semantic-section distribution: minimum ≈ 263, median ≈ "
        "366, p95 ≈ 433, maximum ≈ 480 characters. All current sections "
        "are below the production 800-character fallback threshold.",
        "",
        "## Sample Places",
        "",
        *sample_lines,
        "",
        "## Manual Evaluation Rubric",
        "",
        "Score each configuration manually. Do not infer these scores from "
        "chunk counts alone.",
        "",
        "| Criterion | Recursive 500/50 | Recursive 800/100 | "
        "Markdown Header |",
        "|---|---:|---:|---:|",
        "| Avoids unnatural cuts | 1–3 | 1–3 | 1–3 |",
        "| Topic coherence | 1–3 | 1–3 | 1–3 |",
        "| Context preservation | 1–3 | 1–3 | 1–3 |",
        "| Independently understandable | 1–3 | 1–3 | 1–3 |",
        "| Reasonable chunk count/size | 1–3 | 1–3 | 1–3 |",
        "",
        "Scale: **1 = poor, 2 = acceptable, 3 = good**.",
        "",
    ]


def _configuration_report(
    *,
    configuration: str,
    chunks: list[SampleChunk],
    statistics: ChunkStatistics,
) -> list[str]:
    lines = [
        f"### {CONFIGURATION_HEADINGS[configuration]}",
        "",
        (
            f"Statistics: {statistics.chunk_count} chunks; min "
            f"{statistics.minimum_length}; average "
            f"{statistics.average_length:.1f}; max "
            f"{statistics.maximum_length} characters."
        ),
        "",
    ]

    for index, chunk in enumerate(chunks, start=1):
        lines.extend([f"#### Chunk {index}", ""])
        if chunk.section is not None:
            lines.extend([f"Section: `{chunk.section}`", ""])
        lines.extend(["```text", chunk.content, "```", ""])

    lines.extend(["---", ""])
    return lines


def _summary_report(
    summary: dict[str, ChunkStatistics],
) -> list[str]:
    lines = [
        "## Final Summary",
        "",
        "| Configuration | Total chunks | Avg chunk length |",
        "|---|---:|---:|",
    ]

    for configuration in CONFIGURATION_ORDER:
        statistics = summary[configuration]
        lines.append(
            f"| {configuration} | {statistics.chunk_count} | "
            f"{statistics.average_length:.1f} |"
        )

    return lines


def _ensure_non_empty_chunks(chunks: list[SampleChunk]) -> None:
    if not chunks:
        raise ValueError("A chunking configuration produced no chunks")

    if any(not chunk.content.strip() for chunk in chunks):
        raise ValueError("A chunking configuration produced an empty chunk")


if __name__ == "__main__":
    main()
