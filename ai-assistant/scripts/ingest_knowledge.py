import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.knowledge_base import knowledge_base


if __name__ == "__main__":
    document_count, chunk_count = knowledge_base.rebuild()
    print(f"indexed documents={document_count}, chunks={chunk_count}")
