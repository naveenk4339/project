from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql://rag:rag@localhost:5432/rag"

    llm_provider: str = "claude"  # claude | openai | gemini
    llm_model: str = ""

    embedding_provider: str = "openai"  # openai | gemini | hash
    embedding_model: str = ""
    embedding_dim: int = 1536

    chunk_size: int = 1000
    chunk_overlap: int = 150
    top_k: int = 5


@lru_cache
def get_settings() -> Settings:
    return Settings()
