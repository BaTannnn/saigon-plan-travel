from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, model_validator

KnowledgeSection = Literal[
    "OVERVIEW",
    "BACKGROUND",
    "HIGHLIGHTS",
    "EXPERIENCE",
]


class Source(BaseModel):
    model_config = ConfigDict(extra="forbid")

    label: str = Field(min_length=1, max_length=255)
    uri: HttpUrl
    retrievedAt: date


class CorpusSection(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunkIndex: int = Field(ge=1)
    section: KnowledgeSection
    content: str = Field(min_length=50, max_length=3000)
    source: Source


class PlaceCorpus(BaseModel):
    model_config = ConfigDict(extra="forbid")

    placeSlug: str = Field(
        min_length=1,
        max_length=180,
        pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$",
    )

    language: Literal["vi-VN"]

    sections: list[CorpusSection] = Field(
        min_length=1,
        max_length=20,
    )

    @model_validator(mode="after")
    def validate_sections(self):
        indexes = [section.chunkIndex for section in self.sections]

        expected = list(range(1, len(indexes) + 1))

        if indexes != expected:
            raise ValueError("chunkIndex must be unique and continuous starting from 1")

        section_names = [section.section for section in self.sections]

        if len(section_names) != len(set(section_names)):
            raise ValueError("semantic section names must be unique")

        return self
