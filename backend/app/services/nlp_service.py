import logging
import json
from typing import Dict, Any, List, Optional
from openai import AsyncOpenAI
from app.core.config import settings

logger = logging.getLogger(__name__)


class NLPService:
    """
    AI analysis using OpenAI Chat Completions API.
    Supports any OpenAI-compatible endpoint via OPENAI_BASE_URL:
    - OpenAI (default, kosongkan OPENAI_BASE_URL)
    - Ollama: http://localhost:11434/v1
    - OpenRouter: https://openrouter.ai/api/v1
    - Groq: https://api.groq.com/openai/v1
    - vLLM, LM Studio, dll
    """

    def __init__(self):
        kwargs = {"api_key": settings.OPENAI_API_KEY}
        if settings.OPENAI_BASE_URL:
            kwargs["base_url"] = settings.OPENAI_BASE_URL
        self.client = AsyncOpenAI(**kwargs)
        self.model = settings.OPENAI_MODEL

    def _supports_json_mode(self) -> bool:
        """
        response_format json_object tidak didukung semua model/provider.
        Dinonaktifkan untuk Ollama dan model lokal.
        """
        if settings.OPENAI_BASE_URL and "ollama" in settings.OPENAI_BASE_URL.lower():
            return False
        if settings.OPENAI_BASE_URL and "localhost:11434" in settings.OPENAI_BASE_URL:
            return False
        return True

    async def analyze_transcript(
        self,
        transcript: str,
        title: str,
        participants: Optional[List[str]] = None,
        language: str = "id",
    ) -> Dict[str, Any]:
        """
        Full analysis: summary, key points, action items, conclusions, sentiment.
        Returns structured dict.
        """
        participant_str = ", ".join(participants) if participants else "tidak diketahui"

        lang_instruction = "Bahasa Indonesia" if language == "id" else "English"

        system_prompt = f"""Kamu adalah asisten analis rapat profesional yang membantu mengekstrak informasi penting dari transkrip rapat.
Selalu jawab dalam {lang_instruction}.
Berikan output yang terstruktur, ringkas, dan actionable."""

        user_prompt = f"""Analisis transkrip rapat berikut:

JUDUL RAPAT: {title}
PESERTA: {participant_str}

TRANSKRIP:
{transcript}

Berikan analisis lengkap dalam format JSON dengan struktur PERSIS seperti ini:
{{
  "summary": "Rangkuman eksekutif 3-5 paragraf dari keseluruhan diskusi",
  "key_points": [
    "Poin kunci 1",
    "Poin kunci 2",
    "Poin kunci 3"
  ],
  "action_items": [
    {{
      "task": "Deskripsi tugas",
      "assignee": "Nama PIC atau null",
      "due_date": "Tenggat waktu atau null"
    }}
  ],
  "conclusions": "Sintesis akhir dan keputusan yang diambil dalam rapat",
  "sentiment": {{
    "overall": "positive|neutral|negative",
    "score": 0.0,
    "topics": {{
      "topik_utama": "positive|neutral|negative"
    }}
  }},
  "keywords": ["kata1", "kata2", "kata3", "kata4", "kata5"]
}}

Pastikan JSON valid dan lengkap."""

        try:
            create_kwargs = dict(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=settings.OPENAI_MAX_TOKENS,
                temperature=0.3,
            )
            if self._supports_json_mode():
                create_kwargs["response_format"] = {"type": "json_object"}

            response = await self.client.chat.completions.create(**create_kwargs)

            content = response.choices[0].message.content
            result = json.loads(content)

            # Ensure required fields exist
            return {
                "summary": result.get("summary", ""),
                "key_points": result.get("key_points", []),
                "action_items": result.get("action_items", []),
                "conclusions": result.get("conclusions", ""),
                "sentiment": result.get("sentiment", {"overall": "neutral", "score": 0.0, "topics": {}}),
                "keywords": result.get("keywords", []),
            }

        except Exception as e:
            logger.error(f"NLP analysis failed: {e}")
            return {
                "summary": transcript[:500] + "..." if len(transcript) > 500 else transcript,
                "key_points": [],
                "action_items": [],
                "conclusions": "",
                "sentiment": {"overall": "neutral", "score": 0.0, "topics": {}},
                "keywords": [],
            }

    async def generate_slide_content(
        self,
        summary: str,
        key_points: List[str],
        action_items: List[Dict],
        title: str,
        language: str = "id",
    ) -> List[Dict[str, Any]]:
        """Generate structured slide content for PPTX."""
        lang_instruction = "Bahasa Indonesia" if language == "id" else "English"

        user_prompt = f"""Buat konten slide presentasi dari rangkuman rapat berikut.
Jawab dalam {lang_instruction} dalam format JSON.

JUDUL: {title}
RANGKUMAN: {summary}
POIN KUNCI: {json.dumps(key_points, ensure_ascii=False)}
ACTION ITEMS: {json.dumps(action_items, ensure_ascii=False)}

Format output:
{{
  "slides": [
    {{
      "title": "Judul slide",
      "bullets": ["poin 1", "poin 2"],
      "notes": "catatan presenter opsional"
    }}
  ]
}}

Buat 5-8 slide yang logis dan informatif."""

        try:
            slide_kwargs = dict(
                model=self.model,
                messages=[{"role": "user", "content": user_prompt}],
                max_tokens=2000,
                temperature=0.4,
            )
            if self._supports_json_mode():
                slide_kwargs["response_format"] = {"type": "json_object"}

            response = await self.client.chat.completions.create(**slide_kwargs)
            result = json.loads(response.choices[0].message.content)
            return result.get("slides", [])
        except Exception as e:
            logger.error(f"Slide generation failed: {e}")
            return [{"title": title, "bullets": key_points[:5], "notes": ""}]


nlp_service = NLPService()
