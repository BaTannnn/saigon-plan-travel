from dataclasses import dataclass

from langchain_core.documents import Document
from langchain_text_splitters import (
    MarkdownHeaderTextSplitter,
    RecursiveCharacterTextSplitter,
)

from app.knowledge.corpus_schema import (
    KnowledgeSection,
    PlaceCorpus,
    Source,
)

MARKDOWN_HEADERS = (
    ("#", "place"),
    ("##", "section"),
)
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
    recursive_splitter = create_recursive_splitter()
    prepared_chunks: list[PreparedKnowledgeChunk] = []

    for section in corpus.sections:
        content = section.content.strip()

        if not content:
            raise ValueError("Chunk content must not be empty")

        parts = [content]

        if len(content) > SECTION_CHUNK_SIZE:
            parts = recursive_splitter.split_text(content)

        for part in parts:
            prepared_chunks.append(
                PreparedKnowledgeChunk(
                    chunk_index=len(prepared_chunks) + 1,
                    place_slug=corpus.placeSlug,
                    language=corpus.language,
                    section=section.section,
                    content=part.strip(),
                    source=section.source,
                )
            )

    return prepared_chunks
