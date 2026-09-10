from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ConversationMessage(BaseModel):
    model_config = ConfigDict(extra="forbid")

    role: Literal["USER", "ASSISTANT"]
    content: str = Field(min_length=1, max_length=2000)

    @field_validator("content")
    @classmethod
    def normalize_content(cls, content: str) -> str:
        normalized = content.strip()
        if not normalized:
            raise ValueError("content must not be blank")
        return normalized


class AssistantRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    message: str = Field(min_length=3, max_length=1000)
    history: list[ConversationMessage] = Field(default_factory=list, max_length=10)
    excluded_place_slugs: list[str] = Field(default_factory=list)

    @field_validator("message")
    @classmethod
    def normalize_message(cls, message: str) -> str:
        normalized = message.strip()
        if len(normalized) < 3:
            raise ValueError("message must contain at least 3 non-blank characters")
        return normalized

    @field_validator("excluded_place_slugs")
    @classmethod
    def normalize_excluded_place_slugs(cls, slugs: list[str]) -> list[str]:
        return list(dict.fromkeys(slug.strip() for slug in slugs if slug.strip()))


class GeneratedPlaceRecommendation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    slug: str = Field(min_length=1)
    reason: str = Field(min_length=1, max_length=500)

    @field_validator("slug", "reason")
    @classmethod
    def reject_blank_generated_value(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("generated value must not be blank")
        return normalized


class GeneratedAssistantOutput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    answer: str = Field(min_length=1, max_length=5000)
    suggested_places: list[GeneratedPlaceRecommendation] = Field(max_length=10)

    @field_validator("answer")
    @classmethod
    def reject_blank_answer(cls, answer: str) -> str:
        normalized = answer.strip()
        if not normalized:
            raise ValueError("answer must not be blank")
        return normalized


class PlaceSource(BaseModel):
    type: Literal["PLACE"] = "PLACE"
    place_slug: str
    place_name: str
    section: str


class DocumentSource(BaseModel):
    type: Literal["DOCUMENT"] = "DOCUMENT"
    title: str
    page_number: int
    source_label: str | None = None


class AssistantResponse(BaseModel):
    answer: str
    suggested_places: list[GeneratedPlaceRecommendation]
    sources: list[PlaceSource | DocumentSource]
