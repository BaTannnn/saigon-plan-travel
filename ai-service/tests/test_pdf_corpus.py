import hashlib
from pathlib import Path

import pytest
from pypdf import PdfWriter
from pypdf.errors import PdfReadError

from app.knowledge import pdf_corpus
from app.knowledge.pdf_corpus import (
    ExtractedPdfPage,
    PdfCorpusError,
    prepare_pdf_document,
    split_pdf_pages,
)


class FakePage:
    def __init__(self, content: str | None):
        self.content = content

    def extract_text(self) -> str | None:
        return self.content


class FakeMetadata:
    title = "  Cẩm nang   TP.HCM  "


class FakeReader:
    is_encrypted = False
    metadata = FakeMetadata()

    def __init__(self, _path: Path):
        self.pages = [FakePage("Trang một"), FakePage(None), FakePage("Trang ba")]


def make_text_pdf(text: str) -> bytes:
    text_stream = f"BT /F1 12 Tf 72 720 Td ({text}) Tj ET".encode("ascii")
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        (
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            b"/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>"
        ),
        b"<< /Length "
        + str(len(text_stream)).encode("ascii")
        + b" >>\nstream\n"
        + text_stream
        + b"\nendstream",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]
    pdf = bytearray(b"%PDF-1.4\n")
    offsets = [0]

    for number, body in enumerate(objects, start=1):
        offsets.append(len(pdf))
        pdf.extend(f"{number} 0 obj\n".encode("ascii"))
        pdf.extend(body)
        pdf.extend(b"\nendobj\n")

    xref_offset = len(pdf)
    pdf.extend(b"xref\n0 6\n0000000000 65535 f \n")
    for offset in offsets[1:]:
        pdf.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    pdf.extend(b"trailer\n<< /Size 6 /Root 1 0 R >>\n")
    pdf.extend(f"startxref\n{xref_offset}\n%%EOF\n".encode("ascii"))
    return bytes(pdf)


def test_prepare_pdf_extracts_text_and_preserves_original_page_numbers(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    path = tmp_path / "guide.pdf"
    content = b"fake-pdf-content"
    path.write_bytes(content)
    monkeypatch.setattr(pdf_corpus, "PdfReader", FakeReader)

    document = prepare_pdf_document(
        path,
        source_uri="corpus/documents/guide.pdf",
    )

    assert document.title == "Cẩm nang TP.HCM"
    assert document.source_label == "Cẩm nang TP.HCM"
    assert document.source_uri == "corpus/documents/guide.pdf"
    assert document.content_hash == hashlib.sha256(content).hexdigest()
    assert document.total_pages == 3
    assert [page.page_number for page in document.pages] == [1, 3]
    assert [chunk.page_number for chunk in document.chunks] == [1, 3]


def test_prepare_pdf_extracts_text_with_pypdf(tmp_path: Path) -> None:
    path = tmp_path / "real.pdf"
    path.write_bytes(make_text_pdf("Ho Chi Minh City travel guide"))

    document = prepare_pdf_document(
        path,
        source_uri="corpus/documents/real.pdf",
    )

    assert document.total_pages == 1
    assert document.pages[0].content == "Ho Chi Minh City travel guide"
    assert document.chunks[0].page_number == 1


def test_split_pdf_pages_keeps_every_chunk_on_its_source_page() -> None:
    pages = [
        ExtractedPdfPage(page_number=7, content="Đoạn văn dài. " * 100),
        ExtractedPdfPage(page_number=8, content="Nội dung trang kế tiếp."),
    ]

    chunks = split_pdf_pages(pages, title="Cẩm nang")

    assert len(chunks) > 2
    assert chunks[0].chunk_index == 1
    assert chunks[-1].chunk_index == len(chunks)
    assert all(chunk.page_number == 7 for chunk in chunks[:-1])
    assert chunks[-1].page_number == 8
    assert all(len(chunk.content_hash) == 64 for chunk in chunks)


def test_prepare_pdf_rejects_empty_file(tmp_path: Path) -> None:
    path = tmp_path / "empty.pdf"
    path.touch()

    with pytest.raises(PdfCorpusError, match="PDF is empty"):
        prepare_pdf_document(path, source_uri="corpus/documents/empty.pdf")


def test_prepare_pdf_rejects_unreadable_file(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    path = tmp_path / "broken.pdf"
    path.write_bytes(b"not-a-pdf")

    def raise_read_error(_path: Path):
        raise PdfReadError("broken")

    monkeypatch.setattr(pdf_corpus, "PdfReader", raise_read_error)

    with pytest.raises(PdfCorpusError, match="Unreadable PDF"):
        prepare_pdf_document(path, source_uri="corpus/documents/broken.pdf")


def test_prepare_pdf_rejects_pdf_without_extractable_text(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    path = tmp_path / "scanned.pdf"
    path.write_bytes(b"fake-pdf-content")

    class EmptyReader(FakeReader):
        def __init__(self, _path: Path):
            self.pages = [FakePage(None), FakePage("  ")]

    monkeypatch.setattr(pdf_corpus, "PdfReader", EmptyReader)

    with pytest.raises(PdfCorpusError, match="no extractable text"):
        prepare_pdf_document(path, source_uri="corpus/documents/scanned.pdf")


def test_prepare_pdf_rejects_password_protected_pdf(tmp_path: Path) -> None:
    path = tmp_path / "protected.pdf"
    writer = PdfWriter()
    writer.add_blank_page(width=200, height=200)
    writer.encrypt("secret")
    with path.open("wb") as output:
        writer.write(output)

    with pytest.raises(PdfCorpusError, match="Encrypted PDF cannot be read"):
        prepare_pdf_document(path, source_uri="corpus/documents/protected.pdf")
