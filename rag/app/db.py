import re
from dataclasses import dataclass

import psycopg
from pgvector import Vector
from pgvector.psycopg import register_vector
from psycopg_pool import ConnectionPool

from .config import Settings


@dataclass
class Hit:
    document: str
    chunk_index: int
    content: str
    score: float


class Store:
    def __init__(self, settings: Settings):
        self.dsn = settings.database_url
        self.dim = settings.embedding_dim
        self.pool_size = settings.db_pool_size
        self.pool: ConnectionPool | None = None

    def open(self) -> None:
        """Create the schema, then open the connection pool (the vector type must exist first)."""
        self.init_schema()
        self.pool = ConnectionPool(
            self.dsn, min_size=1, max_size=self.pool_size, configure=_configure, open=True
        )

    def close(self) -> None:
        if self.pool is not None:
            self.pool.close()
            self.pool = None

    def conn(self):
        if self.pool is None:
            raise RuntimeError("Store is not open; call open() first")
        return self.pool.connection()

    def init_schema(self) -> None:
        with psycopg.connect(self.dsn) as conn:
            conn.execute("CREATE EXTENSION IF NOT EXISTS vector")
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    id          SERIAL PRIMARY KEY,
                    filename    TEXT UNIQUE NOT NULL,
                    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
                )"""
            )
            conn.execute(
                f"""
                CREATE TABLE IF NOT EXISTS chunks (
                    id          BIGSERIAL PRIMARY KEY,
                    document_id INT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                    chunk_index INT NOT NULL,
                    content     TEXT NOT NULL,
                    embedding   vector({self.dim}) NOT NULL
                )"""
            )
            existing = conn.execute(
                "SELECT format_type(atttypid, atttypmod) FROM pg_attribute "
                "WHERE attrelid = 'chunks'::regclass AND attname = 'embedding'"
            ).fetchone()[0]
            m = re.search(r"\((\d+)\)", existing)
            if m and int(m.group(1)) != self.dim:
                raise RuntimeError(
                    f"chunks.embedding is vector({m.group(1)}) but EMBEDDING_DIM={self.dim}. "
                    "Changing embedding model/dimension requires re-ingesting: DROP TABLE chunks, documents; then restart."
                )
            # HNSW supports up to 2000 dims; above that we fall back to exact (sequential) search.
            if self.dim <= 2000:
                conn.execute(
                    "CREATE INDEX IF NOT EXISTS chunks_embedding_hnsw "
                    "ON chunks USING hnsw (embedding vector_cosine_ops)"
                )

    def replace_document(self, filename: str, chunks: list[str], embeddings: list[list[float]]) -> int:
        """Insert (or fully replace) a document and its chunks in one transaction."""
        with self.conn() as conn, conn.transaction():
            conn.execute("DELETE FROM documents WHERE filename = %s", (filename,))
            doc_id = conn.execute(
                "INSERT INTO documents (filename) VALUES (%s) RETURNING id", (filename,)
            ).fetchone()[0]
            with conn.cursor() as cur:
                cur.executemany(
                    "INSERT INTO chunks (document_id, chunk_index, content, embedding) VALUES (%s, %s, %s, %s)",
                    [(doc_id, i, c, Vector(e)) for i, (c, e) in enumerate(zip(chunks, embeddings))],
                )
        return doc_id

    def search(self, embedding: list[float], k: int) -> list[Hit]:
        with self.conn() as conn:
            rows = conn.execute(
                """
                SELECT d.filename, c.chunk_index, c.content, 1 - (c.embedding <=> %(q)s) AS score
                FROM chunks c JOIN documents d ON d.id = c.document_id
                ORDER BY c.embedding <=> %(q)s
                LIMIT %(k)s""",
                {"q": Vector(embedding), "k": k},
            ).fetchall()
        return [Hit(*r) for r in rows]

    def list_documents(self) -> list[dict]:
        with self.conn() as conn:
            rows = conn.execute(
                """
                SELECT d.id, d.filename, d.created_at, count(c.id)
                FROM documents d LEFT JOIN chunks c ON c.document_id = d.id
                GROUP BY d.id ORDER BY d.filename"""
            ).fetchall()
        return [{"id": r[0], "filename": r[1], "created_at": r[2], "chunks": r[3]} for r in rows]

    def delete_document(self, doc_id: int) -> bool:
        with self.conn() as conn:
            return conn.execute("DELETE FROM documents WHERE id = %s", (doc_id,)).rowcount > 0


def _configure(conn: psycopg.Connection) -> None:
    register_vector(conn)
    conn.commit()  # register_vector queries the type OID; don't hand the pool a connection mid-transaction
