import unittest
from datetime import date

from pydantic import ValidationError

from app.knowledge.corpus_schema import CorpusSection, PlaceCorpus, Source


CONTENT = (
    "Nội dung thử nghiệm đủ dài để thỏa mãn giới hạn của một semantic "
    "section trong corpus địa điểm."
)


def section(index: int, name: str) -> CorpusSection:
    return CorpusSection(
        chunkIndex=index,
        section=name,
        content=CONTENT,
        source=Source(
            label="Test source",
            uri="https://example.com/place",
            retrievedAt=date(2026, 8, 25),
        ),
    )


class CorpusSchemaTest(unittest.TestCase):

    def test_rejects_ambiguous_duplicate_semantic_sections(self):
        with self.assertRaisesRegex(
            ValidationError,
            "semantic section names must be unique",
        ):
            PlaceCorpus(
                placeSlug="test-place",
                language="vi-VN",
                sections=[
                    section(1, "OVERVIEW"),
                    section(2, "OVERVIEW"),
                ],
            )

    def test_rejects_non_continuous_source_section_indexes(self):
        with self.assertRaisesRegex(
            ValidationError,
            "chunkIndex must be unique and continuous starting from 1",
        ):
            PlaceCorpus(
                placeSlug="test-place",
                language="vi-VN",
                sections=[
                    section(1, "OVERVIEW"),
                    section(3, "BACKGROUND"),
                ],
            )


if __name__ == "__main__":
    unittest.main()
