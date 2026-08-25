import unittest
from datetime import date
from pathlib import Path

from app.rag.chunking import (
    SECTION_CHUNK_SIZE,
    build_place_markdown,
    split_place_corpus,
    split_place_markdown,
)
from app.rag.corpus_schema import CorpusSection, PlaceCorpus, Source


CORPUS_DIR = Path(__file__).resolve().parents[1] / "corpus"
DEFAULT_CONTENT = {
    "OVERVIEW": (
        "Tổng quan đủ dài mô tả địa điểm thử nghiệm và các đặc điểm "
        "chính dành cho khách tham quan tại Thành phố Hồ Chí Minh."
    ),
    "BACKGROUND": (
        "Bối cảnh đủ dài trình bày lịch sử hình thành và vai trò của "
        "địa điểm trong đời sống đô thị qua nhiều giai đoạn."
    ),
    "HIGHLIGHTS": (
        "Điểm nổi bật đủ dài mô tả kiến trúc, cảnh quan và các chi tiết "
        "đáng chú ý mà khách tham quan có thể quan sát."
    ),
    "EXPERIENCE": (
        "Trải nghiệm đủ dài mô tả những hoạt động phù hợp và cách khách "
        "tham quan có thể tìm hiểu không gian địa điểm."
    ),
}


def source(section: str) -> Source:
    return Source(
        label=f"Source {section}",
        uri=f"https://example.com/{section.lower()}",
        retrievedAt=date(2026, 8, 25),
    )


def corpus(
    *,
    content_by_section: dict[str, str] | None = None,
) -> PlaceCorpus:
    contents = {**DEFAULT_CONTENT, **(content_by_section or {})}
    return PlaceCorpus(
        placeSlug="test-place",
        language="vi-VN",
        sections=[
            CorpusSection(
                chunkIndex=index,
                section=section,
                content=contents[section],
                source=source(section),
            )
            for index, section in enumerate(contents, start=1)
        ],
    )


class ChunkingTest(unittest.TestCase):

    def test_builds_canonical_markdown_in_validated_section_order(self):
        place = corpus()

        markdown = build_place_markdown(place)

        expected_headers = [
            "# test-place",
            "## OVERVIEW",
            "## BACKGROUND",
            "## HIGHLIGHTS",
            "## EXPERIENCE",
        ]
        header_positions = [markdown.index(header) for header in expected_headers]
        self.assertEqual(sorted(header_positions), header_positions)

        for section in place.sections:
            self.assertIn(section.content, markdown)

        self.assertNotIn("Source OVERVIEW", markdown)
        self.assertNotIn("https://example.com", markdown)
        self.assertNotIn("2026-08-25", markdown)

    def test_header_splitter_preserves_place_and_section_semantics(self):
        place = corpus()

        documents = split_place_markdown(build_place_markdown(place))

        self.assertEqual(4, len(documents))
        for section, document in zip(place.sections, documents):
            self.assertEqual(
                {
                    "place": place.placeSlug,
                    "section": section.section,
                },
                document.metadata,
            )
            self.assertEqual(section.content, document.page_content)
            self.assertNotIn("##", document.page_content)

    def test_recursive_fallback_splits_only_oversized_section(self):
        long_background = "\n\n".join(
            f"Đoạn lịch sử {index} cung cấp dữ liệu có căn cứ về địa điểm."
            for index in range(40)
        )
        place = corpus(
            content_by_section={"BACKGROUND": long_background}
        )

        chunks = split_place_corpus(place)
        overview_chunks = [
            chunk for chunk in chunks if chunk.section == "OVERVIEW"
        ]
        background_chunks = [
            chunk for chunk in chunks if chunk.section == "BACKGROUND"
        ]

        self.assertEqual(1, len(overview_chunks))
        self.assertGreater(len(background_chunks), 1)
        self.assertTrue(
            all(len(chunk.content) <= SECTION_CHUNK_SIZE for chunk in chunks)
        )

        background_source = place.sections[1].source
        for chunk in background_chunks:
            self.assertEqual("test-place", chunk.place_slug)
            self.assertEqual("vi-VN", chunk.language)
            self.assertEqual(background_source, chunk.source)

        for index in range(40):
            self.assertTrue(
                any(
                    f"Đoạn lịch sử {index} " in chunk.content
                    for chunk in background_chunks
                )
            )

    def test_final_chunk_indexes_are_continuous_and_deterministic(self):
        long_background = " ".join(
            f"Nội dung lịch sử phần {index} có thông tin rõ ràng."
            for index in range(60)
        )
        place = corpus(
            content_by_section={"BACKGROUND": long_background}
        )

        first = split_place_corpus(place)
        second = split_place_corpus(place)

        self.assertEqual(first, second)
        self.assertEqual(
            list(range(1, len(first) + 1)),
            [chunk.chunk_index for chunk in first],
        )
        self.assertEqual("OVERVIEW", first[0].section)
        background_indexes = [
            chunk.chunk_index
            for chunk in first
            if chunk.section == "BACKGROUND"
        ]
        self.assertGreater(len(background_indexes), 1)
        first_highlight = next(
            chunk.chunk_index
            for chunk in first
            if chunk.section == "HIGHLIGHTS"
        )
        self.assertEqual(background_indexes[-1] + 1, first_highlight)

    def test_current_corpus_sections_remain_intact(self):
        corpus_files = sorted(CORPUS_DIR.glob("*.json"))
        prepared_count = 0

        for path in corpus_files:
            place = PlaceCorpus.model_validate_json(
                path.read_text(encoding="utf-8")
            )
            chunks = split_place_corpus(place)
            self.assertEqual(len(place.sections), len(chunks))
            for source_section, chunk in zip(place.sections, chunks):
                self.assertEqual(
                    source_section.chunkIndex,
                    chunk.chunk_index,
                )
                self.assertEqual(source_section.section, chunk.section)
                self.assertEqual(source_section.content, chunk.content)
            prepared_count += len(chunks)

        self.assertEqual(20, len(corpus_files))
        self.assertEqual(80, prepared_count)


if __name__ == "__main__":
    unittest.main()
