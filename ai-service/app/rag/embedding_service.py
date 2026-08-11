from google import genai
from google.genai import types


EMBEDDING_MODEL = "gemini-embedding-2"
EMBEDDING_DIMENSION = 768


def create_client() -> genai.Client:
    return genai.Client()


def prepare_document(
    content: str,
    title: str | None = None,
) -> str:
    document_title = title or "none"

    return (
        f"title: {document_title} | "
        f"text: {content.strip()}"
    )


def prepare_query(query: str) -> str:
    return f"task: search result | query: {query.strip()}"


def embed_document(
    content: str,
    title: str | None = None,
) -> list[float] | None:
    client = create_client()

    prepared_content = prepare_document(
        content=content,
        title=title,
    )

    result = client.models.embed_content(
        model=EMBEDDING_MODEL,
        contents=prepared_content,
        config=types.EmbedContentConfig(
            output_dimensionality=EMBEDDING_DIMENSION
        ),
    )

    return result.embeddings[0].values


def embed_query(query: str) -> list[float] | None:
    client = create_client()

    prepared_query = prepare_query(query)

    result = client.models.embed_content(
        model=EMBEDDING_MODEL,
        contents=prepared_query,
        config=types.EmbedContentConfig(
            output_dimensionality=EMBEDDING_DIMENSION
        ),
    )

    return result.embeddings[0].values