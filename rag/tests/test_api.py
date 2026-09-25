"""End-to-end test against a real Postgres+pgvector (set TEST_DATABASE_URL), with offline
hash embeddings and a stub LLM so no API keys are needed."""
import os
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

DB = os.environ.get("TEST_DATABASE_URL")
pytestmark = pytest.mark.skipif(not DB, reason="TEST_DATABASE_URL not set")
DOCS = Path(__file__).resolve().parent.parent / "docs"


class StubLLM:
    model = "stub"

    def __init__(self):
        self.prompts = []

    def generate(self, system, prompt):
        self.prompts.append(prompt)
        return "stub answer [1]"


@pytest.fixture
def client(monkeypatch):
    import psycopg

    monkeypatch.setenv("DATABASE_URL", DB)
    monkeypatch.setenv("EMBEDDING_PROVIDER", "hash")
    monkeypatch.setenv("EMBEDDING_DIM", "1024")
    with psycopg.connect(DB) as conn:
        conn.execute("DROP TABLE IF EXISTS chunks, documents")

    from app import main
    from app.config import get_settings

    get_settings.cache_clear()
    stub = StubLLM()
    monkeypatch.setattr(main, "get_llm", lambda s: stub)
    with TestClient(main.app) as c:
        c.stub = stub
        yield c
    get_settings.cache_clear()


def test_ingest_search_ask(client):
    files = [("files", (p.name, p.read_bytes(), "text/plain")) for p in sorted(DOCS.glob("*.txt"))]
    r = client.post("/documents", files=files)
    assert r.status_code == 201, r.text
    assert len(r.json()["ingested"]) == len(files)

    docs = client.get("/documents").json()
    assert {d["filename"] for d in docs} == {p.name for p in DOCS.glob("*.txt")}

    hits = client.post("/search", json={"question": "battery warranty miles capacity", "top_k": 3}).json()
    assert "03-warranty-policy.txt" in {h["document"] for h in hits}

    r = client.post("/ask", json={"question": "How long is the battery warranty?"})
    assert r.status_code == 200
    body = r.json()
    assert body["answer"] == "stub answer [1]"
    assert body["sources"][0]["ref"] == 1
    assert "03-warranty-policy.txt" in client.stub.prompts[0]


def test_reupload_replaces_and_delete(client):
    one = DOCS / "01-company-overview.txt"
    for _ in range(2):
        client.post("/documents", files=[("files", (one.name, one.read_bytes(), "text/plain"))])
    docs = client.get("/documents").json()
    assert len(docs) == 1
    assert client.delete(f"/documents/{docs[0]['id']}").status_code == 204
    assert client.get("/documents").json() == []


def test_rejects_unsupported_type(client):
    r = client.post("/documents", files=[("files", ("x.docx", b"data", "application/octet-stream"))])
    assert r.status_code == 400
