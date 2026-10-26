"""Bulk-ingest a folder: python -m app.ingest_cli docs/"""
import sys
from pathlib import Path

from .config import get_settings
from .db import Store
from .embeddings import get_embedder
from .loaders import SUPPORTED
from .rag import RagService


def main(folder: str) -> None:
    settings = get_settings()
    store = Store(settings)
    store.open()
    service = RagService(settings, store, get_embedder(settings), llm=None)  # ingest doesn't need the LLM
    files = sorted(p for p in Path(folder).iterdir() if p.suffix.lower() in SUPPORTED)
    if not files:
        sys.exit(f"No {sorted(SUPPORTED)} files in {folder}")
    try:
        for path in files:
            result = service.ingest(path.name, path.read_bytes())
            print(f"{result['filename']}: {result['chunks']} chunks")
    finally:
        store.close()


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "docs")
