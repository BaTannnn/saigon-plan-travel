from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, ConfigDict, Field, ValidationError

from app.rag.embedding_service import embed_query
from app.rag.knowledge_repository import (
    KnowledgeChunk,
    find_relevant_chunks_by_place_slugs,
)
from app.rag.rag_service import create_chat_model


class GeneratedItineraryReason(BaseModel):
    model_config = ConfigDict(extra="forbid")

    place_slug: str
    reason: str = Field(min_length=1, max_length=500)


class GeneratedItineraryReasons(BaseModel):
    model_config = ConfigDict(extra="forbid")

    reasons: list[GeneratedItineraryReason]


class ItineraryReason(BaseModel):
    place_slug: str
    reason: str | None


INSUFFICIENT_CONTEXT_REASON = (
    "Dữ liệu hiện có chưa đủ để giải thích đề xuất này."
)
DEFAULT_CHUNKS_PER_PLACE = 2


PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """
Bạn viết lý do đề xuất cho các địa điểm đã được hệ thống xếp lịch.

Quy tắc bắt buộc:
- Chỉ sử dụng thông tin có trong CONTEXT được cung cấp.
- Không chọn, loại bỏ hoặc thay đổi thứ tự địa điểm.
- Không tự bịa sự kiện, giờ mở cửa, giá, đánh giá, lịch sử, tiện ích hoặc đặc điểm.
- Với mỗi địa điểm, giải thích ngắn gọn địa điểm liên quan thế nào đến sở thích của người dùng.
- Lý do phải phù hợp để hiển thị trên thẻ hành trình và viết bằng tiếng Việt.
- Trả về đúng một lý do cho mỗi place slug trong PLACE SLUGS.
- Giữ nguyên chính xác place slug được cung cấp.
- Nếu context không đủ bằng chứng, dùng câu trung lập:
  "Dữ liệu hiện có chưa đủ để giải thích đề xuất này."
            """.strip(),
        ),
        (
            "human",
            """
PREFERENCE:
{preference}

PLACE SLUGS:
{place_slugs}

CONTEXT:
{context}
            """.strip(),
        ),
    ]
)


def build_itinerary_context(chunks: list[KnowledgeChunk]) -> str:
    parts: list[str] = []

    for index, chunk in enumerate(chunks, start=1):
        parts.append(
            f"""
[CHUNK {index}]
Place: {chunk.place_name}
Place slug: {chunk.place_slug}
Chunk index: {chunk.chunk_index}
Section: {chunk.section}
Content: {chunk.content}
Source: {chunk.source_label}
Source URI: {chunk.source_uri}
            """.strip()
        )

    return "\n\n".join(parts)


def generate_itinerary_reasons(
    preference: str,
    place_slugs: list[str],
    chunks_per_place: int = DEFAULT_CHUNKS_PER_PLACE,
) -> list[ItineraryReason]:
    unique_slugs = list(dict.fromkeys(place_slugs))
    if not unique_slugs:
        return []

    if chunks_per_place <= 0:
        raise ValueError("chunks_per_place must be greater than zero")

    try:
        preference_embedding = embed_query(preference)
    except Exception:
        return _insufficient_context_reasons(unique_slugs)

    if preference_embedding is None:
        return _insufficient_context_reasons(unique_slugs)

    retrieved_chunks = find_relevant_chunks_by_place_slugs(
        query_embedding=preference_embedding,
        place_slugs=unique_slugs,
        chunks_per_place=chunks_per_place,
    )
    chunks = _bounded_requested_chunks(
        chunks=retrieved_chunks,
        requested_slugs=unique_slugs,
        chunks_per_place=chunks_per_place,
    )
    slugs_with_context = {chunk.place_slug for chunk in chunks}
    llm_slugs = [
        slug for slug in unique_slugs if slug in slugs_with_context
    ]

    if not chunks:
        return _insufficient_context_reasons(unique_slugs)

    reasons_by_slug: dict[str, str] = {}
    context = build_itinerary_context(chunks)

    structured_model = create_chat_model().with_structured_output(
        GeneratedItineraryReasons
    )
    response = structured_model.invoke(
        PROMPT.format_messages(
            preference=preference,
            place_slugs="\n".join(llm_slugs),
            context=context,
        )
    )

    try:
        parsed = (
            response
            if isinstance(response, GeneratedItineraryReasons)
            else GeneratedItineraryReasons.model_validate(response)
        )
    except ValidationError as exception:
        raise ValueError("Malformed itinerary explanation output") from exception

    for item in parsed.reasons:
        if item.place_slug not in slugs_with_context:
            raise ValueError(
                "Itinerary explanation output contains an unknown place slug"
            )
        if item.place_slug in reasons_by_slug:
            raise ValueError(
                "Itinerary explanation output contains duplicate place slugs"
            )
        reason = item.reason.strip()
        if not reason:
            raise ValueError(
                "Itinerary explanation output contains a blank reason"
            )
        reasons_by_slug[item.place_slug] = reason

    if set(reasons_by_slug) != slugs_with_context:
        raise ValueError(
            "Itinerary explanation output is missing a requested place slug"
        )

    return [
        ItineraryReason(
            place_slug=slug,
            reason=(
                reasons_by_slug[slug]
                if slug in slugs_with_context
                else INSUFFICIENT_CONTEXT_REASON
            ),
        )
        for slug in unique_slugs
    ]


def _bounded_requested_chunks(
    *,
    chunks: list[KnowledgeChunk],
    requested_slugs: list[str],
    chunks_per_place: int,
) -> list[KnowledgeChunk]:
    requested = set(requested_slugs)
    counts_by_slug: dict[str, int] = {}
    selected: list[KnowledgeChunk] = []

    for chunk in chunks:
        if chunk.place_slug not in requested:
            continue

        current_count = counts_by_slug.get(chunk.place_slug, 0)
        if current_count >= chunks_per_place:
            continue

        selected.append(chunk)
        counts_by_slug[chunk.place_slug] = current_count + 1

    return selected


def _insufficient_context_reasons(
    place_slugs: list[str],
) -> list[ItineraryReason]:
    return [
        ItineraryReason(
            place_slug=slug,
            reason=INSUFFICIENT_CONTEXT_REASON,
        )
        for slug in place_slugs
    ]
