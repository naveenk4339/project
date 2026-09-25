import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from .config import get_settings
from .db import Store
from .embeddings import get_embedder
from .llm import LLMError, get_llm
from .loaders import SUPPORTED
from .rag import RagService

log = logging.getLogger("rag")
FRONTEND = Path(__file__).resolve().parent.parent / "frontend" / "index.html"
MAX_UPLOAD_BYTES = 25 * 1024 * 1024

rag: RagService


@asynccontextmanager
async def lifespan(app: FastAPI):
    global rag
    settings = get_settings()
    store = Store(settings)
    store.init_schema()
    rag = RagService(settings, store, get_embedder(settings), get_llm(settings))
    log.info("RAG ready: llm=%s/%s embeddings=%s dim=%d",
             settings.llm_provider, rag.llm.model, settings.embedding_provider, settings.embedding_dim)
    yield


app = FastAPI(title="RAG API", version="1.0.0", lifespan=lifespan)


class AskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=4000)
    top_k: int | None = Field(default=None, ge=1, le=20)


class SearchRequest(AskRequest):
    pass


@app.get("/", include_in_schema=False)
def index():
    return FileResponse(FRONTEND)


@app.get("/health")
def health():
    s = get_settings()
    return {"status": "ok", "llm": f"{s.llm_provider}/{rag.llm.model}", "embeddings": s.embedding_provider}


@app.post("/documents", status_code=201)
async def upload(files: list[UploadFile] = File(...)):
    results = []
    for f in files:
        name = Path(f.filename or "").name
        if Path(name).suffix.lower() not in SUPPORTED:
            raise HTTPException(400, f"{name!r}: unsupported type, expected {sorted(SUPPORTED)}")
        data = await f.read()
        if len(data) > MAX_UPLOAD_BYTES:
            raise HTTPException(413, f"{name!r} exceeds {MAX_UPLOAD_BYTES // 2**20} MB")
        try:
            results.append(await run_in_threadpool(rag.ingest, name, data))
        except ValueError as e:
            raise HTTPException(400, str(e)) from e
    return {"ingested": results}


@app.get("/documents")
def list_documents():
    return rag.store.list_documents()


@app.delete("/documents/{doc_id}", status_code=204)
def delete_document(doc_id: int):
    if not rag.store.delete_document(doc_id):
        raise HTTPException(404, "Document not found")


@app.post("/search")
def search(req: SearchRequest):
    """Retrieval only - returns the top matching chunks without calling the LLM."""
    return [h.__dict__ for h in rag.retrieve(req.question, req.top_k)]


@app.post("/ask")
def ask(req: AskRequest):
    try:
        return rag.ask(req.question, req.top_k)
    except LLMError as e:
        raise HTTPException(422, str(e)) from e
    except Exception as e:  # provider auth/network/rate-limit errors
        log.exception("LLM/embedding call failed")
        raise HTTPException(502, f"Upstream model provider error: {type(e).__name__}: {e}") from e
