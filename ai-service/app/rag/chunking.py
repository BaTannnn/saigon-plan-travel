from dataclasses import dataclass

from langchain_core.documents import Document
from langchain_text_splitters import (
    MarkdownHeaderTextSplitter,
    RecursiveCharacterTextSplitter,
)

from app.rag.corpus_schema import (
    KnowledgeSection,
    PlaceCorpus,
    Source,
)


MARKDOWN_HEADERS = (
    ("#", "place"),
    ("##", "section"),
)

# Measured with len() over the 80 current corpus sections: median 366,
# p95 433, maximum 480 characters. 800 keeps every current semantic section
# intact while leaving room for corpus growth. The 100-character overlap is
# applied only when a section exceeds that boundary.
SECTION_CHUNK_SIZE = 800
SECTION_CHUNK_OVERLAP = 100
RECURSIVE_SEPARATORS = ("\n\n", "\n", " ", "")


@dataclass(frozen=True)
class PreparedKnowledgeChunk:
    chunk_index: int
    place_slug: str
    language: str
    section: KnowledgeSection
    content: str
    source: Source


def build_place_markdown(corpus: PlaceCorpus) -> str:
    blocks = [f"# {corpus.placeSlug}"]

    for section in corpus.sections:
        blocks.append(
            f"## {section.section}\n{section.content.strip()}"
        )

    return "\n\n".join(blocks) + "\n"


def split_place_markdown(markdown: str) -> list[Document]:
    splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=list(MARKDOWN_HEADERS),
        strip_headers=True,
    )
    return splitter.split_text(markdown)


def create_recursive_splitter() -> RecursiveCharacterTextSplitter:
    return RecursiveCharacterTextSplitter(
        chunk_size=SECTION_CHUNK_SIZE,
        chunk_overlap=SECTION_CHUNK_OVERLAP,
        length_function=len,
        separators=list(RECURSIVE_SEPARATORS),
        keep_separator=True,
        strip_whitespace=True,
    )


def split_place_corpus(
    corpus: PlaceCorpus,
) -> list[PreparedKnowledgeChunk]:
    section_documents = split_place_markdown(
        build_place_markdown(corpus)
    )
    expected_sections = [section.section for section in corpus.sections]
    actual_sections = [
        document.metadata.get("section")
        for document in section_documents
    ]

    if actual_sections != expected_sections:
        raise ValueError(
            "Markdown splitting did not preserve semantic section order"
        )

    source_by_section = {
        section.section: section.source
        for section in corpus.sections
    }
    recursive_splitter = create_recursive_splitter()
    prepared_chunks: list[PreparedKnowledgeChunk] = []

    for section_document in section_documents:
        place_slug = section_document.metadata.get("place")
        section_name = section_document.metadata.get("section")

        if place_slug != corpus.placeSlug:
            raise ValueError(
                "Markdown splitting did not preserve place metadata"
            )

        if section_name not in source_by_section:
            raise ValueError(
                "Markdown splitting produced an unknown semantic section"
            )

        final_documents = [section_document]
        if len(section_document.page_content) > SECTION_CHUNK_SIZE:
            final_documents = recursive_splitter.split_documents(
                [section_document]
            )

        for final_document in final_documents:
            content = final_document.page_content.strip()

            if not content:
                raise ValueError("Chunk content must not be empty")

            if final_document.metadata != section_document.metadata:
                raise ValueError(
                    "Recursive splitting did not preserve header metadata"
                )

            prepared_chunks.append(
                PreparedKnowledgeChunk(
                    chunk_index=len(prepared_chunks) + 1,
                    place_slug=corpus.placeSlug,
                    language=corpus.language,
                    section=section_name,
                    content=content,
                    source=source_by_section[section_name],
                )
            )

    return prepared_chunks
