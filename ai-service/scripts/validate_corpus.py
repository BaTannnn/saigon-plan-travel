from pathlib import Path

from pydantic import ValidationError

from app.knowledge.corpus_schema import PlaceCorpus


CORPUS_DIR = Path("corpus")


def validate_file(path: Path) -> bool:
    try:
        content = path.read_text(encoding="utf-8")

        corpus = PlaceCorpus.model_validate_json(content)

        print(
            f"[OK] {path.name}: "
            f"{corpus.placeSlug} - "
            f"{len(corpus.sections)} semantic sections"
        )

        return True

    except ValidationError as exception:
        print(f"[INVALID] {path.name}")
        print(exception)

        return False


def main() -> None:
    files = sorted(CORPUS_DIR.glob("*.json"))

    if not files:
        print("No corpus files found.")
        return

    failed = 0

    for path in files:
        if not validate_file(path):
            failed += 1

    print()
    print(f"Validated: {len(files)}")
    print(f"Failed: {failed}")

    if failed > 0:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
