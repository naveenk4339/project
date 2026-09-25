"""Chat providers. Each takes a system prompt + user prompt and returns the answer text."""
from typing import Protocol

from .config import Settings


class LLMError(RuntimeError):
    pass


class LLM(Protocol):
    model: str

    def generate(self, system: str, prompt: str) -> str: ...


class ClaudeLLM:
    def __init__(self, settings: Settings):
        import anthropic

        self.client = anthropic.Anthropic()
        self.model = settings.llm_model or "claude-opus-5"

    def generate(self, system: str, prompt: str) -> str:
        response = self.client.beta.messages.create(
            model=self.model,
            max_tokens=16000,
            system=system,
            messages=[{"role": "user", "content": prompt}],
            # If a safety classifier declines, re-run server-side on Anthropic's recommended fallback model.
            betas=["server-side-fallback-2026-07-01"],
            extra_body={"fallbacks": "default"},
        )
        if response.stop_reason == "refusal":
            raise LLMError("The model declined to answer this request.")
        return "".join(b.text for b in response.content if b.type == "text").strip()


class OpenAILLM:
    def __init__(self, settings: Settings):
        from openai import OpenAI

        self.client = OpenAI()
        self.model = settings.llm_model or "gpt-5-mini"

    def generate(self, system: str, prompt: str) -> str:
        resp = self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": prompt}],
        )
        return (resp.choices[0].message.content or "").strip()


class GeminiLLM:
    def __init__(self, settings: Settings):
        from google import genai

        self.client = genai.Client()
        self.model = settings.llm_model or "gemini-2.5-flash"

    def generate(self, system: str, prompt: str) -> str:
        from google.genai import types

        resp = self.client.models.generate_content(
            model=self.model,
            contents=prompt,
            config=types.GenerateContentConfig(system_instruction=system),
        )
        return (resp.text or "").strip()


def get_llm(settings: Settings) -> LLM:
    providers = {"claude": ClaudeLLM, "openai": OpenAILLM, "gemini": GeminiLLM}
    try:
        return providers[settings.llm_provider.lower()](settings)
    except KeyError:
        raise ValueError(f"Unknown LLM_PROVIDER {settings.llm_provider!r}") from None
