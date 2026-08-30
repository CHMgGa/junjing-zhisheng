from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class EnvironmentSnapshot(BaseModel):
    model_config = ConfigDict(extra="ignore")

    greenhouse_id: int | None = None
    greenhouse_name: str | None = None
    air_temperature: float | None = None
    air_humidity: float | None = None
    soil_temperature: float | None = None
    soil_humidity: float | None = None
    ph_value: float | None = None
    temperature: float | None = None
    humidity: float | None = None
    co2_ppm: float | None = None
    soil_moisture: float | None = None
    light_lux: float | None = None


class ChatRequest(BaseModel):
    question: str = Field(min_length=1)
    greenhouse_id: int | None = None
    environment: EnvironmentSnapshot | None = None


class VisionDiagnosisRequest(BaseModel):
    question: str | None = None
    image_base64: str = Field(min_length=1)
    image_filename: str | None = None
    greenhouse_id: int | None = None
    environment: EnvironmentSnapshot | None = None


class ReferenceChunk(BaseModel):
    title: str
    source: str
    page: int | None = None
    content: str
    score: float | None = None


class AiResult(BaseModel):
    answer: str
    risk_level: str = "LOW"
    diagnosis: str | None = None
    actions: list[str] = Field(default_factory=list)
    references: list[ReferenceChunk] = Field(default_factory=list)
    expert_trace: list[str] = Field(default_factory=list)
    raw: dict[str, Any] = Field(default_factory=dict)


class SearchRequest(BaseModel):
    query: str = Field(min_length=1)
    top_k: int = 5


class IndexResult(BaseModel):
    document_count: int
    chunk_count: int
