from dataclasses import asdict

from .chunking import chunk_text
from .config import Settings
from .db import Store
from .embeddings import Embedder
from .llm import LLM
from .loaders import extract_text

SYSTEM_PROMPT = """You answer questions using only the context passages provided from the user's document collection.

Each passage is labeled [n] with its source file. Cite the passages you rely on inline, like [1] or [2][3].
If the context does not contain the answer, say you couldn't find it in the documents rather than answering from general knowledge.
Be concise."""


class RagService:
    def __init__(self, settings: Settings, store: Store, embedder: Embedder, llm: LLM | None):
        self.settings = settings
        self.store = store
        self.embedder = embedder
        self.llm = llm

    def ingest(self, filename: str, data: bytes) -> dict:
        text = extract_text(filename, data)
        chunks = chunk_text(text, self.settings.chunk_size, self.settings.chunk_overlap)
        if not chunks:
            raise ValueError(f"No extractable text in {filename} (scanned PDFs need OCR first)")
        embeddings = self.embedder.embed(chunks, task="document")
        doc_id = self.store.replace_document(filename, chunks, embeddings)
        return {"id": doc_id, "filename": filename, "chunks": len(chunks)}

    def retrieve(self, question: str, k: int | None = None):
        [q] = self.embedder.embed([question], task="query")
        return self.store.search(q, k or self.settings.top_k)

    def ask(self, question: str, k: int | None = None) -> dict:
        hits = self.retrieve(question, k)
        if not hits:
            return {"answer": "No documents have been ingested yet.", "sources": [], "model": None}
        context = "\n\n".join(
            f"[{i}] (source: {h.document}, chunk {h.chunk_index})\n{h.content}" for i, h in enumerate(hits, 1)
        )
        prompt = f"<context>\n{context}\n</context>\n\nQuestion: {question}"
        answer = self.llm.generate(SYSTEM_PROMPT, prompt)
        return {
            "answer": answer,
            "model": self.llm.model,
            "sources": [{"ref": i, **asdict(h)} for i, h in enumerate(hits, 1)],
        }
