import os
import tempfile
import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)


class ASRService:
    """
    Automatic Speech Recognition using OpenAI Whisper.
    Loads model lazily on first use to avoid startup delay.
    """

    def __init__(self):
        self._model = None
        self._model_name = None

    def _load_model(self):
        from app.core.config import settings
        import whisper

        if self._model is None or self._model_name != settings.WHISPER_MODEL:
            logger.info(f"Loading Whisper model: {settings.WHISPER_MODEL}")
            self._model = whisper.load_model(
                settings.WHISPER_MODEL,
                device=settings.WHISPER_DEVICE
            )
            self._model_name = settings.WHISPER_MODEL
            logger.info("Whisper model loaded")

        return self._model

    def transcribe(self, audio_data: bytes, language: str = "id") -> Dict[str, Any]:
        """
        Transcribe audio bytes.

        Returns:
            {
                "text": str,
                "segments": [{"speaker": str, "text": str, "start": float, "end": float, "confidence": float}],
                "language": str,
                "duration": float
            }
        """
        model = self._load_model()

        # Write to temp file (Whisper needs file path)
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            tmp.write(audio_data)
            tmp_path = tmp.name

        try:
            # Map language codes
            lang_map = {"id": "indonesian", "en": "english", "jv": "javanese", "zh": "chinese", "ar": "arabic"}
            whisper_lang = lang_map.get(language, language)

            result = model.transcribe(
                tmp_path,
                language=whisper_lang,
                word_timestamps=True,
                verbose=False,
            )

            segments = []
            for i, seg in enumerate(result.get("segments", [])):
                segments.append({
                    "speaker": f"Speaker {(i % 3) + 1}",  # Placeholder; real diarization via pyannote
                    "text": seg["text"].strip(),
                    "start": round(seg["start"], 2),
                    "end": round(seg["end"], 2),
                    "confidence": round(seg.get("avg_logprob", 0) + 1, 2),  # Normalize logprob to ~[0,1]
                })

            return {
                "text": result["text"].strip(),
                "segments": segments,
                "language": result.get("language", language),
                "duration": segments[-1]["end"] if segments else 0.0,
            }

        finally:
            os.unlink(tmp_path)

    def transcribe_chunk(self, audio_chunk: bytes, language: str = "id") -> str:
        """Quick transcription for real-time streaming chunks."""
        result = self.transcribe(audio_chunk, language)
        return result["text"]


asr_service = ASRService()
