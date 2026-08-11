from pathlib import Path

from app.rag.corpus_schema import PlaceCorpus
from app.rag.place_lookup import resolve_place_id


CORPUS_DIR = Path("corpus")


def main() -> None:
    files = sorted(CORPUS_DIR.glob("*.json"))

    if not files:
        print("No corpus files found.")
        return

    missing = 0

    for path in files:
        raw_json = path.read_text(encoding="utf-8")

        corpus = PlaceCorpus.model_validate_json(raw_json)

        place_id = resolve_place_id(corpus.placeSlug)

        if place_id is None:
            print(
                f"[MISSING] {corpus.placeSlug}"
            )
            missing += 1

            continue

        print(
            f"[OK] {corpus.placeSlug}"
            f" -> place_id={place_id}"
        )

    print()
    print(f"Checked: {len(files)}")
    print(f"Missing: {missing}")

    if missing > 0:
        raise SystemExit(1)


if __name__ == "__main__":
    main()