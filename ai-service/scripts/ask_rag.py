from app.db.postgres import pool
from app.rag.rag_service import answer_question


def main() -> None:
    question = input("Question: ").strip()

    if not question:
        return

    result = answer_question(question)

    print()
    print("ANSWER")
    print("------")
    print(result.answer)

    print()
    print("SOURCES")
    print("-------")

    for source in result.sources:
        print(
            f"- {source.place_name} "
            f"[{source.section}] "
            f"similarity={source.similarity:.4f}"
        )

        print(
            f"  {source.source_uri}"
        )


if __name__ == "__main__":
    pool.open()

    try:
        main()
    finally:
        pool.close()