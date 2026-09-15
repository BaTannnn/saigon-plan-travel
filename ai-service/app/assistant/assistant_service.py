from pydantic import ValidationError

from app.assistant.models import (
    AssistantRequest,
    AssistantResponse,
    ConversationMessage,
    GeneratedAssistantOutput,
    GeneratedPlaceRecommendation,
)
from app.assistant.prompts import ASSISTANT_PROMPT
from app.knowledge.embedding_service import embed_query
from app.knowledge.retrieval_repository import (
    DocumentSearchResult,
    SearchResult,
    search_similar_chunks,
    search_similar_document_chunks,
)
from app.rag.rag_service import create_chat_model

DEFAULT_PLACE_TOP_K = 5
DEFAULT_DOCUMENT_TOP_K = 5
RETRIEVAL_USER_HISTORY_LIMIT = 2
INSUFFICIENT_CONTEXT_ANSWER = "Dữ liệu hiện có chưa đủ để trả lời câu hỏi này."


def answer_assistant_message(
    request: AssistantRequest,
    *,
    place_top_k: int = DEFAULT_PLACE_TOP_K,
    document_top_k: int = DEFAULT_DOCUMENT_TOP_K,
) -> AssistantResponse:
    question = request.message.strip()
    retrieval_query = build_retrieval_query(question, request.history)
    query_embedding = embed_query(retrieval_query)
    if query_embedding is None:
        raise RuntimeError("Could not create query embedding")

    place_results = search_similar_chunks(
        query_embedding=query_embedding,
        limit=place_top_k,
    )
    document_results = search_similar_document_chunks(
        query_embedding=query_embedding,
        limit=document_top_k,
    )

    if not place_results and not document_results:
        return AssistantResponse(
            answer=INSUFFICIENT_CONTEXT_ANSWER,
            suggested_places=[],
        )

    excluded_slugs = set(request.excluded_place_slugs)
    allowed_slugs = list(
        dict.fromkeys(
            result.place_slug for result in place_results if result.place_slug not in excluded_slugs
        )
    )
    structured_model = create_chat_model().with_structured_output(GeneratedAssistantOutput)
    response = structured_model.invoke(
        ASSISTANT_PROMPT.format_messages(
            question=question,
            history=_build_history(request),
            allowed_place_slugs="\n".join(allowed_slugs) or "(none)",
            evidence=_build_evidence(place_results, document_results),
        )
    )
    try:
        parsed = (
            response
            if isinstance(response, GeneratedAssistantOutput)
            else GeneratedAssistantOutput.model_validate(response)
        )
    except ValidationError as exception:
        raise ValueError("Malformed assistant structured output") from exception

    allowed_slug_set = set(allowed_slugs)
    seen_slugs: set[str] = set()
    recommendations: list[GeneratedPlaceRecommendation] = []
    for recommendation in parsed.suggested_places:
        slug = recommendation.slug.strip()
        reason = recommendation.reason.strip()
        if slug not in allowed_slug_set or slug in seen_slugs or not reason:
            continue
        seen_slugs.add(slug)
        recommendations.append(GeneratedPlaceRecommendation(slug=slug, reason=reason))

    return AssistantResponse(
        answer=parsed.answer.strip(),
        suggested_places=recommendations,
    )


def build_retrieval_query(
    current_message: str,
    history: list[ConversationMessage],
) -> str:
    recent_user_messages = [item.content for item in history if item.role == "USER"][
        -RETRIEVAL_USER_HISTORY_LIMIT:
    ]
    return "\n".join([*recent_user_messages, current_message])


def _build_history(request: AssistantRequest) -> str:
    if not request.history:
        return "(empty)"
    return "\n".join(f"[{item.role}] {item.content.strip()}" for item in request.history)


def _build_evidence(
    place_results: list[SearchResult],
    document_results: list[DocumentSearchResult],
) -> str:
    parts: list[str] = []
    for index, result in enumerate(place_results, start=1):
        parts.append(
            "\n".join(
                [
                    f"[PLACE {index}]",
                    f"Name: {result.place_name}",
                    f"Slug: {result.place_slug}",
                    f"Section: {result.section}",
                    f"Content: {result.content}",
                    f"Source label: {result.source_label}",
                ]
            )
        )
    for index, result in enumerate(document_results, start=1):
        parts.append(
            "\n".join(
                [
                    f"[DOCUMENT {index}]",
                    f"Title: {result.document_title}",
                    f"Page: {result.page_number}",
                    f"Content: {result.content}",
                    f"Source label: {result.source_label}",
                ]
            )
        )
    return "\n\n".join(parts)
