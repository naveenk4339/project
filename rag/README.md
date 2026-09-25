# RAG stack: FastAPI + PostgreSQL/pgvector

A small retrieval-augmented generation (RAG) service. Upload 5–10 PDF or text files, then ask questions. Answers are grounded in the retrieved chunks and cite them.

| Layer      | Choice |
|------------|--------|
| Frontend   | Single-file React page at `/` (`frontend/index.html`) **or** the Postman collection in `postman/` |
| Backend    | Python FastAPI (`app/`) |
| LLM        | Claude (default, `claude-opus-5`), OpenAI or Gemini, picked with `LLM_PROVIDER` |
| Embeddings | OpenAI (`text-embedding-3-small`) or Gemini (`gemini-embedding-001`), picked with `EMBEDDING_PROVIDER` |
| Vector DB  | PostgreSQL 16 + pgvector (HNSW index, cosine distance) |
| Documents  | `docs/` has 6 sample text files; upload your own `.pdf` / `.txt` / `.md` |

Anthropic has no embeddings endpoint, so if you use Claude for answers, use OpenAI or Gemini for embeddings.

## How it works

```
upload ─► extract text (pypdf) ─► chunk (~1000 chars, 150 overlap) ─► embed ─► INSERT INTO chunks (pgvector)
question ─► embed ─► ORDER BY embedding <=> query LIMIT k ─► prompt LLM with numbered passages ─► answer + sources
```

## Run it

### Option A: Docker Compose (Postgres + API)

```bash
cd rag
cp .env.example .env          # set provider(s) and API key(s)
docker compose up --build
docker compose exec api python -m app.ingest_cli docs    # load the sample docs
```

Open http://localhost:8000 for the UI, or http://localhost:8000/docs for Swagger.

### Option B: Local Python + Dockerized Postgres

```bash
cd rag
docker compose up -d db
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env          # edit keys
python -m app.ingest_cli docs
uvicorn app.main:app --reload
```

### Provider configurations (`.env`)

| Setup | Settings |
|-------|----------|
| Claude + OpenAI embeddings | `LLM_PROVIDER=claude` `EMBEDDING_PROVIDER=openai` `EMBEDDING_DIM=1536`, `ANTHROPIC_API_KEY`, `OPENAI_API_KEY` |
| Claude + Gemini embeddings | `LLM_PROVIDER=claude` `EMBEDDING_PROVIDER=gemini` `EMBEDDING_DIM=768`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY` |
| All OpenAI | `LLM_PROVIDER=openai` `EMBEDDING_PROVIDER=openai` `EMBEDDING_DIM=1536`, `OPENAI_API_KEY` |
| All Gemini | `LLM_PROVIDER=gemini` `EMBEDDING_PROVIDER=gemini` `EMBEDDING_DIM=768`, `GEMINI_API_KEY` |
| Offline smoke test | `EMBEDDING_PROVIDER=hash` (keyword-only; retrieval works, not semantic) |

`LLM_MODEL` / `EMBEDDING_MODEL` override the defaults. **Changing the embedding model or dimension means re-ingesting.** Drop the tables (`DROP TABLE chunks, documents;`) and run the ingest again; the app refuses to start if the column dimension doesn't match `EMBEDDING_DIM`.

With Claude, the request opts into server-side refusal fallbacks (`fallbacks: "default"`), so a request declined by a safety classifier is retried on Anthropic's recommended fallback model. A refusal that still happens returns HTTP 422.

## API

| Method | Path | Body | Purpose |
|--------|------|------|---------|
| GET    | `/health` | – | Provider/model info |
| POST   | `/documents` | multipart `files` (one or more) | Ingest; re-uploading a filename replaces it |
| GET    | `/documents` | – | List documents and chunk counts |
| DELETE | `/documents/{id}` | – | Remove a document and its chunks |
| POST   | `/search` | `{"question": "...", "top_k": 5}` | Retrieval only, no LLM |
| POST   | `/ask` | `{"question": "...", "top_k": 5}` | Retrieve + generate an answer with cited sources |

```bash
curl -F files=@docs/03-warranty-policy.txt http://localhost:8000/documents
curl -X POST http://localhost:8000/ask -H 'Content-Type: application/json' \
     -d '{"question": "How long is the battery warranty?"}'
```

Example `/ask` response:

```json
{
  "answer": "The battery and drive unit are covered for 8 years or 120,000 miles, with at least 70% capacity retained [1].",
  "model": "claude-opus-5",
  "sources": [{"ref": 1, "document": "03-warranty-policy.txt", "chunk_index": 0, "content": "...", "score": 0.61}]
}
```

Postman: import `postman/RAG.postman_collection.json`; `baseUrl` defaults to `http://localhost:8000`.

## Tests

```bash
pytest                                            # chunking unit tests
TEST_DATABASE_URL=postgresql://rag:rag@localhost:5432/rag pytest   # + end-to-end API tests
```

The end-to-end tests use the offline `hash` embedder and a stub LLM, so they need a pgvector database but no API keys. **They drop and recreate the `chunks`/`documents` tables**, so point them at a scratch database.

## Notes and limits

- Scanned PDFs have no text layer; run OCR first.
- Ingestion is synchronous. That's fine for 5–10 documents; for large batches, move it to a background worker.
- There is no auth. Don't expose the service publicly as is.
