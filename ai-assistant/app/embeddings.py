import hashlib
import math
import re


class HashEmbeddingFunction:
    def __init__(self, dimensions: int = 384):
        self.dimensions = dimensions

    def __call__(self, input: list[str]) -> list[list[float]]:
        return [self.embed(text) for text in input]

    def name(self) -> str:
        return "morel-hash-embedding"

    def embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        tokens = re.findall(r"[\w\u4e00-\u9fff]+", text.lower())
        for token in tokens:
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "little") % self.dimensions
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            vector[index] += sign
        norm = math.sqrt(sum(value * value for value in vector)) or 1.0
        return [value / norm for value in vector]
