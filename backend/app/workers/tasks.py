import logging
import asyncio
from typing import List, Optional
from celery import shared_task
from sqlalchemy.orm import Session

from app.workers.celery_app import celery_app
from app.core.config import settings

logger = logging.getLogger(__name__)


def _get_sync_db():
    """Sync DB session for Celery workers."""
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker
    engine = create_engine(settings.SYNC_DATABASE_URL)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    return SessionLocal()


@celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
def transcribe_audio(self, session_id: str, audio_key: str, language: str = "id"):
    """
    Task: Download audio from storage, run Whisper ASR, save transcript.
    """
    from app.models.session import Session as SessionModel, SessionStatus
    from app.services.asr_service import asr_service
    from app.services.storage_service import storage_service

    db = _get_sync_db()
    try:
        session = db.query(SessionModel).filter(SessionModel.id == session_id).first()
        if not session:
            logger.error(f"Session {session_id} not found")
            return

        # Update status
        session.status = SessionStatus.PROCESSING
        db.commit()

        # Download audio
        logger.info(f"Downloading audio for session {session_id}")
        audio_data = storage_service.download_audio(audio_key)

        # Transcribe
        logger.info(f"Transcribing audio for session {session_id}")
        result = asr_service.transcribe(audio_data, language=language)

        # Save results
        session.transcript = result["text"]
        session.transcript_segments = result["segments"]
        session.audio_duration_seconds = int(result["duration"])
        db.commit()

        logger.info(f"Transcription done for session {session_id}")

        # Chain NLP analysis
        analyze_transcript.delay(session_id)

    except Exception as exc:
        logger.error(f"Transcription failed for {session_id}: {exc}")
        session = db.query(SessionModel).filter(SessionModel.id == session_id).first()
        if session:
            from app.models.session import SessionStatus
            session.status = SessionStatus.FAILED
            db.commit()
        raise self.retry(exc=exc)
    finally:
        db.close()


@celery_app.task(bind=True, max_retries=3, default_retry_delay=60)
def analyze_transcript(self, session_id: str):
    """
    Task: Run NLP analysis on transcript, save summary/action items/etc.
    """
    from app.models.session import Session as SessionModel, SessionStatus
    from app.services.nlp_service import nlp_service

    db = _get_sync_db()
    try:
        session = db.query(SessionModel).filter(SessionModel.id == session_id).first()
        if not session or not session.transcript:
            logger.error(f"Session {session_id} has no transcript")
            return

        participants = [p.get("name") for p in (session.participants or []) if p.get("name")]

        logger.info(f"Analyzing transcript for session {session_id}")
        result = asyncio.run(
            nlp_service.analyze_transcript(
                transcript=session.transcript,
                title=session.title,
                participants=participants,
                language=session.language,
            )
        )

        session.summary = result["summary"]
        session.key_points = result["key_points"]
        session.action_items = result["action_items"]
        session.conclusions = result["conclusions"]
        session.sentiment = result["sentiment"]
        session.status = SessionStatus.COMPLETED
        db.commit()

        logger.info(f"NLP analysis done for session {session_id}")

    except Exception as exc:
        logger.error(f"NLP analysis failed for {session_id}: {exc}")
        raise self.retry(exc=exc)
    finally:
        db.close()


@celery_app.task(bind=True, max_retries=3, default_retry_delay=30)
def generate_documents(self, session_id: str, doc_ids: List[str]):
    """
    Task: Generate PDF/DOCX/PPTX documents for a session.
    """
    from app.models.session import Session as SessionModel
    from app.models.document import Document, DocumentStatus, DocumentFormat
    from app.services.document_service import document_service
    from app.services.nlp_service import nlp_service
    from app.services.storage_service import storage_service

    db = _get_sync_db()
    try:
        session = db.query(SessionModel).filter(SessionModel.id == session_id).first()
        if not session:
            return

        participants = [p.get("name") for p in (session.participants or []) if p.get("name")]
        key_points = session.key_points or []
        action_items = session.action_items or []

        for doc_id in doc_ids:
            doc = db.query(Document).filter(Document.id == doc_id).first()
            if not doc:
                continue

            try:
                doc.status = DocumentStatus.GENERATING
                db.commit()

                fmt = doc.format.value

                # Generate slide content for PPTX
                slides_content = None
                if fmt == "pptx" and session.summary:
                    slides_content = asyncio.run(
                        nlp_service.generate_slide_content(
                            summary=session.summary or "",
                            key_points=key_points,
                            action_items=action_items,
                            title=session.title,
                            language=session.language,
                        )
                    )

                # Generate document
                kwargs = dict(
                    title=session.title,
                    summary=session.summary or "",
                    key_points=key_points,
                    action_items=action_items,
                    conclusions=session.conclusions or "",
                    transcript=session.transcript or "",
                    participants=participants,
                    theme=doc.theme,
                )

                if fmt == "pdf":
                    data = document_service.generate_pdf(**kwargs)
                elif fmt == "docx":
                    data = document_service.generate_docx(**kwargs)
                elif fmt == "pptx":
                    pptx_kwargs = {k: v for k, v in kwargs.items() if k != "transcript"}
                    pptx_kwargs["slides_content"] = slides_content
                    data = document_service.generate_pptx(**pptx_kwargs)
                else:
                    continue

                # Upload to storage
                file_key = storage_service.upload_document(data, session_id, fmt)
                download_url = storage_service.get_document_url(file_key, expires_seconds=86400)

                doc.file_key = file_key
                doc.file_size_bytes = len(data)
                doc.download_url = download_url
                doc.status = DocumentStatus.COMPLETED
                db.commit()

                logger.info(f"Document {doc_id} ({fmt}) generated for session {session_id}")

            except Exception as doc_exc:
                logger.error(f"Failed to generate doc {doc_id}: {doc_exc}")
                doc.status = DocumentStatus.FAILED
                doc.error_message = str(doc_exc)[:900]
                db.commit()

    except Exception as exc:
        logger.error(f"generate_documents failed for {session_id}: {exc}")
        raise self.retry(exc=exc)
    finally:
        db.close()
