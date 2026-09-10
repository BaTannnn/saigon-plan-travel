from contextlib import nullcontext

import pytest

from app.knowledge import retrieval_repository
from app.knowledge.retrieval_repository import search_similar_document_chunks


class FakeCursor:
    def __init__(self, rows):
        self.rows = rows
        self.query = ""
        self.parameters = []

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def execute(self, query, parameters):
        self.query = query
        self.parameters = parameters

    def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self, cursor):
        self._cursor = cursor

    def cursor(self):
        return self._cursor


def test_document_retrieval_filters_orders_and_returns_source_metadata(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    cursor = FakeCursor(
        [
            (
                4,
                "Cẩm nang TP.HCM",
                15,
                27,
                "Kinh nghiệm tham quan",
                0.91,
                "Cẩm nang TP.HCM",
                "corpus/documents/guide.pdf",
            )
        ]
    )
    connection = FakeConnection(cursor)
    monkeypatch.setattr(
        retrieval_repository,
        "get_connection",
        lambda: nullcontext(connection),
    )

    results = search_similar_document_chunks(
        [0.1, 0.2],
        limit=3,
        document_ids=[4, 8],
    )

    assert "WHERE d.id = ANY(%s)" in cursor.query
    assert "ORDER BY c.embedding <=> %s, d.id, c.chunk_index" in cursor.query
    assert cursor.parameters[1] == [4, 8]
    assert cursor.parameters[-1] == 3
    assert len(results) == 1
    assert results[0].document_id == 4
    assert results[0].document_title == "Cẩm nang TP.HCM"
    assert results[0].page_number == 15
    assert results[0].chunk_index == 27
    assert results[0].source_label == "Cẩm nang TP.HCM"
    assert results[0].source_uri == "corpus/documents/guide.pdf"
    assert results[0].source_type == "DOCUMENT"
    assert results[0].similarity == pytest.approx(0.91)


def test_document_retrieval_rejects_invalid_limit() -> None:
    with pytest.raises(ValueError, match="limit must be greater than zero"):
        search_similar_document_chunks([0.1], limit=0)


def test_document_retrieval_returns_early_for_empty_filter() -> None:
    assert search_similar_document_chunks([0.1], document_ids=[]) == []
