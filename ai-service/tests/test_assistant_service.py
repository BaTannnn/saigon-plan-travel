import pytest
from pydantic import ValidationError

from app.assistant import assistant_service
from app.assistant.models import AssistantRequest, GeneratedAssistantOutput
from app.knowledge.retrieval_repository import DocumentSearchResult, SearchResult


class FakeStructuredModel:
    def __init__(self, response):
        self.response = response
        self.messages = None

    def invoke(self, messages):
        self.messages = messages
        return self.response


class FakeChatModel:
    def __init__(self, structured_model):
        self.structured_model = structured_model

    def with_structured_output(self, schema):
        assert schema is GeneratedAssistantOutput
        return self.structured_model


def assistant_request() -> AssistantRequest:
    return AssistantRequest.model_validate(
        {
            "message": "Tôi thích lịch sử, nên đi đâu?",
            "history": [
                {"role": "USER", "content": "Tôi muốn tham quan lịch sử."},
                {"role": "ASSISTANT", "content": "Tôi sẽ dựa vào dữ liệu hiện có."},
            ],
            "excluded_place_slugs": ["dinh-doc-lap"],
        }
    )


def place_result(slug: str = "bao-tang-lich-su-tphcm") -> SearchResult:
    return SearchResult(
        place_slug=slug,
        place_name="Bảo tàng Lịch sử TP.HCM",
        section="BACKGROUND",
        content="Bảo tàng lưu giữ tư liệu lịch sử.",
        source_label="SaigonPlanTravel corpus",
        source_uri="corpus/place.json",
        similarity=0.91,
    )


def document_result() -> DocumentSearchResult:
    return DocumentSearchResult(
        document_id=4,
        document_title="Cẩm nang du lịch TP.HCM",
        page_number=7,
        chunk_index=9,
        content="Khu trung tâm có nhiều điểm tham quan lịch sử.",
        similarity=0.88,
        source_label="Cẩm nang du lịch TP.HCM",
        source_uri="/private/corpus/guide.pdf",
    )


def test_uses_one_embedding_for_both_sources_and_builds_combined_context(monkeypatch) -> None:
    embedding_calls = []
    place_calls = []
    document_calls = []
    structured = FakeStructuredModel(
        {
            "answer": "Bạn có thể cân nhắc bảo tàng.",
            "suggested_places": [
                {
                    "slug": "bao-tang-lich-su-tphcm",
                    "reason": "Phù hợp sở thích lịch sử.",
                }
            ],
        }
    )
    monkeypatch.setattr(
        assistant_service,
        "embed_query",
        lambda question: embedding_calls.append(question) or [0.1, 0.2],
    )
    monkeypatch.setattr(
        assistant_service,
        "search_similar_chunks",
        lambda **kwargs: place_calls.append(kwargs) or [place_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "search_similar_document_chunks",
        lambda **kwargs: document_calls.append(kwargs) or [document_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "create_chat_model",
        lambda: FakeChatModel(structured),
    )

    response = assistant_service.answer_assistant_message(assistant_request())

    assert embedding_calls == ["Tôi muốn tham quan lịch sử.\nTôi thích lịch sử, nên đi đâu?"]
    assert place_calls[0]["query_embedding"] is document_calls[0]["query_embedding"]
    prompt = "\n".join(message.content for message in structured.messages)
    assert "[PLACE 1]" in prompt
    assert "[DOCUMENT 1]" in prompt
    assert "TRIP:" not in prompt
    assert "CURRENT ITINERARY:" not in prompt
    assert "[USER] Tôi muốn tham quan lịch sử." in prompt
    assert response.suggested_places[0].slug == "bao-tang-lich-su-tphcm"
    assert response.sources[0].type == "PLACE"
    assert response.sources[0].place_slug == "bao-tang-lich-su-tphcm"
    assert response.sources[1].type == "DOCUMENT"
    assert response.sources[1].page_number == 7
    assert not hasattr(response.sources[1], "source_uri")


def test_filters_unknown_and_duplicate_recommendation_slugs(monkeypatch) -> None:
    structured = FakeStructuredModel(
        {
            "answer": "Một gợi ý phù hợp.",
            "suggested_places": [
                {"slug": "unknown", "reason": "Không hợp lệ"},
                {"slug": "bao-tang-lich-su-tphcm", "reason": "Phù hợp"},
                {"slug": "bao-tang-lich-su-tphcm", "reason": "Trùng"},
            ],
        }
    )
    monkeypatch.setattr(assistant_service, "embed_query", lambda _question: [0.1])
    monkeypatch.setattr(
        assistant_service,
        "search_similar_chunks",
        lambda **_kwargs: [place_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "search_similar_document_chunks",
        lambda **_kwargs: [],
    )
    monkeypatch.setattr(
        assistant_service,
        "create_chat_model",
        lambda: FakeChatModel(structured),
    )

    response = assistant_service.answer_assistant_message(assistant_request())

    assert [item.slug for item in response.suggested_places] == ["bao-tang-lich-su-tphcm"]


def test_excludes_current_itinerary_place_from_gemini_candidates_and_output(monkeypatch) -> None:
    request = assistant_request()
    request.excluded_place_slugs = ["bao-tang-lich-su-tphcm"]
    structured = FakeStructuredModel(
        {
            "answer": "Tôi sẽ không đề xuất lại địa điểm đã có.",
            "suggested_places": [
                {"slug": "bao-tang-lich-su-tphcm", "reason": "Đã có trong hành trình"}
            ],
        }
    )
    monkeypatch.setattr(assistant_service, "embed_query", lambda _question: [0.1])
    monkeypatch.setattr(
        assistant_service,
        "search_similar_chunks",
        lambda **_kwargs: [place_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "search_similar_document_chunks",
        lambda **_kwargs: [document_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "create_chat_model",
        lambda: FakeChatModel(structured),
    )

    response = assistant_service.answer_assistant_message(request)

    prompt = "\n".join(message.content for message in structured.messages)
    assert "ALLOWED PLACE SLUGS:\n(none)" in prompt
    assert response.suggested_places == []


def test_retrieval_query_uses_only_latest_two_user_messages() -> None:
    request = AssistantRequest.model_validate(
        {
            "message": "Còn chỗ nào nữa?",
            "history": [
                {"role": "USER", "content": "Tin nhắn người dùng cũ."},
                {"role": "ASSISTANT", "content": "Phản hồi không dùng để retrieval."},
                {"role": "USER", "content": "Tôi thích lịch sử."},
                {"role": "ASSISTANT", "content": "Một số gợi ý."},
                {"role": "USER", "content": "Ưu tiên bảo tàng."},
            ],
            "excluded_place_slugs": [],
        }
    )

    assert assistant_service.build_retrieval_query(request.message, request.history) == (
        "Tôi thích lịch sử.\nƯu tiên bảo tàng.\nCòn chỗ nào nữa?"
    )


def test_empty_retrieval_returns_safe_answer_without_calling_gemini(monkeypatch) -> None:
    monkeypatch.setattr(assistant_service, "embed_query", lambda _question: [0.1])
    monkeypatch.setattr(assistant_service, "search_similar_chunks", lambda **_kwargs: [])
    monkeypatch.setattr(
        assistant_service,
        "search_similar_document_chunks",
        lambda **_kwargs: [],
    )
    monkeypatch.setattr(
        assistant_service,
        "create_chat_model",
        lambda: pytest.fail("Gemini must not be called for empty retrieval"),
    )

    response = assistant_service.answer_assistant_message(assistant_request())

    assert response.answer == assistant_service.INSUFFICIENT_CONTEXT_ANSWER
    assert response.suggested_places == []
    assert response.sources == []


def test_invalid_gemini_structured_output_is_rejected(monkeypatch) -> None:
    structured = FakeStructuredModel({"answer": "Missing recommendations"})
    monkeypatch.setattr(assistant_service, "embed_query", lambda _question: [0.1])
    monkeypatch.setattr(
        assistant_service,
        "search_similar_chunks",
        lambda **_kwargs: [place_result()],
    )
    monkeypatch.setattr(
        assistant_service,
        "search_similar_document_chunks",
        lambda **_kwargs: [],
    )
    monkeypatch.setattr(
        assistant_service,
        "create_chat_model",
        lambda: FakeChatModel(structured),
    )

    with pytest.raises(ValueError, match="Malformed assistant structured output"):
        assistant_service.answer_assistant_message(assistant_request())


def test_request_rejects_invalid_role_and_excessive_history() -> None:
    payload = assistant_request().model_dump()
    payload["history"] = [{"role": "SYSTEM", "content": "bad"}]
    with pytest.raises(ValidationError):
        AssistantRequest.model_validate(payload)

    payload["history"] = [{"role": "USER", "content": "message"}] * 11
    with pytest.raises(ValidationError):
        AssistantRequest.model_validate(payload)
