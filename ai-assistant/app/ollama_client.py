import httpx

from app.settings import settings


class OllamaClient:
    def __init__(self):
        self.base_url = settings.ollama_base_url.rstrip("/")

    async def generate(self, model: str, prompt: str, images: list[str] | None = None, max_tokens: int | None = None) -> str:
        payload = {
            "model": model,
            "prompt": prompt,
            "stream": False,
            "keep_alive": settings.ollama_keep_alive,
            "options": {
                "temperature": 0.2,
                "num_ctx": 4096,
                "num_predict": max_tokens or settings.text_max_tokens,
            },
        }
        if images:
            payload["images"] = images
        async with httpx.AsyncClient(timeout=180) as client:
            response = await client.post(f"{self.base_url}/api/generate", json=payload)
            response.raise_for_status()
            data = response.json()
            return data.get("response", "").strip()


ollama_client = OllamaClient()
