"""Chat providers. Each takes a system prompt + user prompt and returns (or streams) the answer text."""
from collections.abc import Iterator
from typing import Protocol

from .config import Settings


class LLMError(RuntimeError):
    pass


class LLM(Protocol):
    model: str

    def generate(self, system: str, prompt: str) -> str: ...

    def stream(self, system: str, prompt: str) -> Iterator[str]: ...


class ClaudeLLM:
    def __init__(self, settings: Settings):
        import anthropic

        self.client = anthropic.Anthropic()
        self.model = settings.llm_model or "claude-opus-5"

    def _params(self, system: str, prompt: str, max_tokens: int) -> dict:
        return dict(
            model=self.model,
            max_tokens=max_tokens,
            system=system,
            messages=[{"role": "user", "content": prompt}],
            # If a safety classifier declines, re-run server-side on Anthropic's recommended fallback model.
            betas=["server-side-fallback-2026-07-01"],
            extra_body={"fallbacks": "default"},
        )

    def generate(self, system: str, prompt: str) -> str:
        response = self.client.beta.messages.create(**self._params(system, prompt, 16000))
        if response.stop_reason == "refusal":
            raise LLMError("The model declined to answer this request.")
        return "".join(b.text for b in response.content if b.type == "text").strip()

    def stream(self, system: str, prompt: str) -> Iterator[str]:
        with self.client.beta.messages.stream(**self._params(system, prompt, 64000)) as stream:
            yield from stream.text_stream
            final = stream.get_final_message()
        if final.stop_reason == "refusal":
            # Anything already streamed is a partial answer; callers should discard it.
            raise LLMError("The model declined to answer this request.")


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

    def stream(self, system: str, prompt: str) -> Iterator[str]:
        chunks = self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": prompt}],
            stream=True,
        )
        for chunk in chunks:
            if chunk.choices and chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content


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

    def stream(self, system: str, prompt: str) -> Iterator[str]:
        from google.genai import types

        for chunk in self.client.models.generate_content_stream(
            model=self.model,
            contents=prompt,
            config=types.GenerateContentConfig(system_instruction=system),
        ):
            if chunk.text:
                yield chunk.text


def get_llm(settings: Settings) -> LLM:
    providers = {"claude": ClaudeLLM, "openai": OpenAILLM, "gemini": GeminiLLM}
    try:
        return providers[settings.llm_provider.lower()](settings)
    except KeyError:
        raise ValueError(f"Unknown LLM_PROVIDER {settings.llm_provider!r}") from None
