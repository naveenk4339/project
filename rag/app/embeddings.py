"""Embedding providers. Each returns one vector of length settings.embedding_dim per input text."""
import hashlib
import math
import re
from typing import Literal, Protocol

from .config import Settings

Task = Literal["document", "query"]


class Embedder(Protocol):
    def embed(self, texts: list[str], task: Task) -> list[list[float]]: ...


class OpenAIEmbedder:
    def __init__(self, settings: Settings):
        from openai import OpenAI

        self.client = OpenAI()
        self.model = settings.embedding_model or "text-embedding-3-small"
        self.dim = settings.embedding_dim

    def embed(self, texts: list[str], task: Task) -> list[list[float]]:
        out: list[list[float]] = []
        for i in range(0, len(texts), 100):
            resp = self.client.embeddings.create(
                model=self.model, input=texts[i : i + 100], dimensions=self.dim
            )
            out.extend(d.embedding for d in resp.data)
        return out


class GeminiEmbedder:
    def __init__(self, settings: Settings):
        from google import genai

        self.client = genai.Client()
        self.model = settings.embedding_model or "gemini-embedding-001"
        self.dim = settings.embedding_dim

    def embed(self, texts: list[str], task: Task) -> list[list[float]]:
        from google.genai import types

        task_type = "RETRIEVAL_DOCUMENT" if task == "document" else "RETRIEVAL_QUERY"
        out: list[list[float]] = []
        for i in range(0, len(texts), 100):
            resp = self.client.models.embed_content(
                model=self.model,
                contents=texts[i : i + 100],
                config=types.EmbedContentConfig(task_type=task_type, output_dimensionality=self.dim),
            )
            # Truncated Gemini vectors aren't unit-length; normalize so cosine == dot product.
            out.extend(_normalize(e.values) for e in resp.embeddings)
        return out


class HashEmbedder:
    """Offline hashed bag-of-words. Keyword matching only - for smoke tests without API keys."""

    def __init__(self, settings: Settings):
        self.dim = settings.embedding_dim

    def embed(self, texts: list[str], task: Task) -> list[list[float]]:
        return [self._one(t) for t in texts]

    def _one(self, text: str) -> list[float]:
        vec = [0.0] * self.dim
        for tok in re.findall(r"[a-z0-9]+", text.lower()):
            h = int.from_bytes(hashlib.md5(tok.encode()).digest()[:8], "big")
            vec[h % self.dim] += 1.0 if (h >> 63) == 0 else -1.0
        return _normalize(vec)


def _normalize(v: list[float]) -> list[float]:
    n = math.sqrt(sum(x * x for x in v)) or 1.0
    return [x / n for x in v]


def get_embedder(settings: Settings) -> Embedder:
    providers = {"openai": OpenAIEmbedder, "gemini": GeminiEmbedder, "hash": HashEmbedder}
    try:
        return providers[settings.embedding_provider.lower()](settings)
    except KeyError:
        raise ValueError(f"Unknown EMBEDDING_PROVIDER {settings.embedding_provider!r}") from None
