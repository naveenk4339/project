import io
from pathlib import Path

from pypdf import PdfReader

SUPPORTED = {".pdf", ".txt", ".md"}


def extract_text(filename: str, data: bytes) -> str:
    suffix = Path(filename).suffix.lower()
    if suffix == ".pdf":
        reader = PdfReader(io.BytesIO(data))
        return "\n\n".join(page.extract_text() or "" for page in reader.pages)
    if suffix in (".txt", ".md"):
        return data.decode("utf-8", errors="replace")
    raise ValueError(f"Unsupported file type {suffix!r}; expected one of {sorted(SUPPORTED)}")
