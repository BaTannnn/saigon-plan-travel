from dataclasses import dataclass
from pathlib import Path

from pypdf import PdfReader
from pypdf.errors import PdfReadError

from app.knowledge.chunking import create_recursive_splitter
from app.knowledge.content_hash import compute_binary_content_hash
from app.knowledge.embedding_service import compute_document_fingerprint


class PdfCorpusError(ValueError):
    pass


@dataclass(frozen=True)
class ExtractedPdfPage:
    page_number: int
    content: str


@dataclass(frozen=True)
class PreparedDocumentChunk:
    chunk_index: int
    page_number: int
    content: str
    content_hash: str


@dataclass(frozen=True)
class PreparedPdfDocument:
    title: str
    source_label: str
    source_uri: str
    content_hash: str
    total_pages: int
    pages: list[ExtractedPdfPage]
    chunks: list[PreparedDocumentChunk]


def prepare_pdf_document(
    path: Path,
    *,
    source_uri: str,
) -> PreparedPdfDocument:
    pdf_bytes = _read_pdf_bytes(path)
    reader = _open_pdf(path)
    _unlock_pdf(reader, path)

    pages = _extract_pages(reader, path)
    title = _document_title(reader, path)
    chunks = split_pdf_pages(pages, title=title)

    return PreparedPdfDocument(
        title=title,
        source_label=title,
        source_uri=source_uri,
        content_hash=compute_binary_content_hash(pdf_bytes),
        total_pages=len(reader.pages),
        pages=pages,
        chunks=chunks,
    )


def split_pdf_pages(
    pages: list[ExtractedPdfPage],
    *,
    title: str,
) -> list[PreparedDocumentChunk]:
    splitter = create_recursive_splitter()
    chunks: list[PreparedDocumentChunk] = []

    for page in pages:
        content = page.content.strip()
        if not content:
            continue

        parts = splitter.split_text(content)

        for part in parts:
            normalized_part = part.strip()
            if not normalized_part:
                continue

            chunks.append(
                PreparedDocumentChunk(
                    chunk_index=len(chunks) + 1,
                    page_number=page.page_number,
                    content=normalized_part,
                    content_hash=compute_document_fingerprint(
                        content=normalized_part,
                        title=title,
                    ),
                )
            )

    if not chunks:
        raise PdfCorpusError("PDF contains no extractable text")

    return chunks


def _read_pdf_bytes(path: Path) -> bytes:
    try:
        content = path.read_bytes()
    except OSError as exc:
        raise PdfCorpusError(f"Unable to read PDF: {path.name}") from exc

    if not content:
        raise PdfCorpusError(f"PDF is empty: {path.name}")

    return content


def _open_pdf(path: Path) -> PdfReader:
    try:
        return PdfReader(path)
    except (OSError, PdfReadError, ValueError) as exc:
        raise PdfCorpusError(f"Unreadable PDF: {path.name}") from exc


def _unlock_pdf(reader: PdfReader, path: Path) -> None:
    if not reader.is_encrypted:
        return

    try:
        unlocked = reader.decrypt("")
    except Exception as exc:
        raise PdfCorpusError(f"Encrypted PDF cannot be read: {path.name}") from exc

    if not unlocked:
        raise PdfCorpusError(f"Encrypted PDF cannot be read: {path.name}")


def _extract_pages(reader: PdfReader, path: Path) -> list[ExtractedPdfPage]:
    pages: list[ExtractedPdfPage] = []

    try:
        for page_number, page in enumerate(reader.pages, start=1):
            content = (page.extract_text() or "").strip()
            if content:
                pages.append(
                    ExtractedPdfPage(
                        page_number=page_number,
                        content=content,
                    )
                )
    except Exception as exc:
        raise PdfCorpusError(f"Unable to extract text from PDF: {path.name}") from exc

    if not pages:
        raise PdfCorpusError(
            f"PDF contains no extractable text (scanned/image-only PDFs require OCR): {path.name}"
        )

    return pages


def _document_title(reader: PdfReader, path: Path) -> str:
    try:
        metadata_title = reader.metadata.title if reader.metadata else None
    except Exception:
        metadata_title = None

    title = metadata_title or path.stem
    return " ".join(str(title).split())[:255].strip()
