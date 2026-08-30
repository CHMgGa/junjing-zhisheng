from pathlib import Path
from uuid import uuid4

import chromadb
from chromadb.api.models.Collection import Collection
from pypdf import PdfReader

from app.embeddings import HashEmbeddingFunction
from app.schemas import ReferenceChunk
from app.settings import settings


class KnowledgeBase:
    def __init__(self):
        settings.chroma_dir.mkdir(parents=True, exist_ok=True)
        self.client = chromadb.PersistentClient(path=str(settings.chroma_dir))
        self.collection: Collection = self.client.get_or_create_collection(
            name="morel_knowledge",
            embedding_function=HashEmbeddingFunction(),
            metadata={"domain": "morel_greenhouse"},
        )

    def rebuild(self) -> tuple[int, int]:
        try:
            self.client.delete_collection("morel_knowledge")
        except Exception:
            pass
        self.collection = self.client.get_or_create_collection(
            name="morel_knowledge",
            embedding_function=HashEmbeddingFunction(),
            metadata={"domain": "morel_greenhouse"},
        )

        documents: list[str] = []
        ids: list[str] = []
        metadatas: list[dict] = []
        pdf_count = 0
        for pdf_path in sorted(settings.knowledge_dir.glob("*.pdf")):
            pdf_count += 1
            for page_index, text in self._read_pdf_pages(pdf_path):
                for chunk_index, chunk in enumerate(self._split_text(text)):
                    ids.append(str(uuid4()))
                    documents.append(chunk)
                    metadatas.append({
                        "source": pdf_path.name,
                        "title": pdf_path.stem,
                        "page": page_index + 1,
                        "chunk": chunk_index,
                    })
        if documents:
            self.collection.add(ids=ids, documents=documents, metadatas=metadatas)
        return pdf_count, len(documents)

    def search(self, query: str, top_k: int = 5) -> list[ReferenceChunk]:
        result = self.collection.query(query_texts=[query], n_results=top_k)
        docs = result.get("documents", [[]])[0]
        metas = result.get("metadatas", [[]])[0]
        distances = result.get("distances", [[]])[0]
        chunks: list[ReferenceChunk] = []
        for doc, meta, distance in zip(docs, metas, distances):
            chunks.append(ReferenceChunk(
                title=meta.get("title", "羊肚菌知识库"),
                source=meta.get("source", "-"),
                page=meta.get("page"),
                content=doc,
                score=float(1 / (1 + distance)) if distance is not None else None,
            ))
        return chunks

    def _read_pdf_pages(self, path: Path) -> list[tuple[int, str]]:
        reader = PdfReader(str(path))
        pages: list[tuple[int, str]] = []
        for index, page in enumerate(reader.pages):
            text = page.extract_text() or ""
            if text.strip():
                pages.append((index, text))
        return pages

    def _split_text(self, text: str) -> list[str]:
        clean = " ".join(text.split())
        chunks: list[str] = []
        start = 0
        while start < len(clean):
            end = min(start + settings.chunk_size, len(clean))
            chunk = clean[start:end].strip()
            if chunk:
                chunks.append(chunk)
            if end == len(clean):
                break
            start = max(0, end - settings.chunk_overlap)
        return chunks


knowledge_base = KnowledgeBase()
