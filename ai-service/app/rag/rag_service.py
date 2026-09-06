import os
from dataclasses import dataclass

from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI

from app.knowledge.embedding_service import embed_query
from app.knowledge.retrieval_repository import (
    SearchResult,
    search_similar_chunks,
)


@dataclass
class RagSource:
    place_slug: str
    place_name: str
    section: str
    source_label: str
    source_uri: str
    similarity: float


@dataclass
class RagAnswer:
    answer: str
    sources: list[RagSource]


PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """
Bạn là trợ lý tư vấn địa điểm du lịch tại TP.HCM.

Chỉ được trả lời dựa trên CONTEXT được cung cấp.

Quy tắc:
- Không sử dụng kiến thức bên ngoài CONTEXT để bổ sung sự kiện.
- Không tự bịa giá vé, giờ mở cửa, địa chỉ hoặc thông tin khác.
- Nếu CONTEXT không đủ để trả lời, hãy nói rõ:
  "Dữ liệu hiện có chưa đủ để trả lời câu hỏi này."
- Có thể tổng hợp và diễn giải lại thông tin từ nhiều đoạn context.
- Trả lời bằng tiếng Việt, ngắn gọn và rõ ràng.
            """.strip(),
        ),
        (
            "human",
            """
QUESTION:
{question}

CONTEXT:
{context}
            """.strip(),
        ),
    ]
)


def create_chat_model() -> ChatGoogleGenerativeAI:
    model_name = os.getenv(
        "GEMINI_CHAT_MODEL",
        "gemini-3.5-flash-lite",
    )

    return ChatGoogleGenerativeAI(
        model=model_name,
        temperature=0,
    )


def build_context(
    results: list[SearchResult],
) -> str:
    parts: list[str] = []

    for index, result in enumerate(
        results,
        start=1,
    ):
        parts.append(
            f"""
                [CHUNK {index}]
                Place: {result.place_name}
                Place slug: {result.place_slug}
                Section: {result.section}
                Content: {result.content}
                Source: {result.source_label}
                Source URI: {result.source_uri}
                            """.strip()
        )

    return "\n\n".join(parts)


def answer_question(
    question: str,
    top_k: int = 5,
) -> RagAnswer:
    query_embedding = embed_query(question)

    results = search_similar_chunks(
        query_embedding=query_embedding,
        limit=top_k,
    )

    if not results:
        return RagAnswer(
            answer=("Dữ liệu hiện có chưa đủ để trả lời câu hỏi này."),
            sources=[],
        )

    context = build_context(results)

    chain = PROMPT | create_chat_model()

    response = chain.invoke(
        {
            "question": question,
            "context": context,
        }
    )

    sources = [
        RagSource(
            place_slug=result.place_slug,
            place_name=result.place_name,
            section=result.section,
            source_label=result.source_label,
            source_uri=result.source_uri,
            similarity=result.similarity,
        )
        for result in results
    ]

    return RagAnswer(
        answer=str(response.content),
        sources=sources,
    )
