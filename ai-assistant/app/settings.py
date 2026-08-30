from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    ollama_base_url: str = "http://localhost:11434"
    text_model: str = "qwen3:4b-thinking"
    vision_model: str = "minicpm-v:latest"
    knowledge_dir: Path = Path("knowledge")
    chroma_dir: Path = Path("data/chroma")
    upload_dir: Path = Path("data/uploads")
    chunk_size: int = 900
    chunk_overlap: int = 160
    text_max_tokens: int = 700
    vision_max_tokens: int = 450
    ollama_keep_alive: str = "10m"

    model_config = SettingsConfigDict(env_prefix="MOREL_AI_", env_file=".env")


settings = Settings()
