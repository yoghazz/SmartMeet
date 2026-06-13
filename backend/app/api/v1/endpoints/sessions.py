import uuid
from typing import List, Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc

from app.core.database import get_db
from app.core.config import settings
from app.models.user import User
from app.models.session import Session, SessionStatus, SessionCategory
from app.schemas.session import SessionCreate, SessionUpdate, SessionResponse, SessionListResponse
from app.api.deps import get_current_user
from app.services.storage_service import storage_service
from app.workers.tasks import transcribe_audio

router = APIRouter(prefix="/sessions", tags=["sessions"])


@router.post("", response_model=SessionResponse, status_code=status.HTTP_201_CREATED)
async def create_session(
    data: SessionCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a new recording session."""
    session = Session(
        owner_id=current_user.id,
        title=data.title,
        category=data.category,
        language=data.language,
        participants=[p.model_dump() for p in data.participants] if data.participants else [],
        recording_mode=data.recording_mode,
        status=SessionStatus.CREATED,
    )
    db.add(session)
    await db.flush()
    return session


@router.get("", response_model=List[SessionListResponse])
async def list_sessions(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
    category: Optional[SessionCategory] = Query(None),
    status: Optional[SessionStatus] = Query(None),
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100),
):
    """List all sessions for current user with optional filters."""
    query = select(Session).where(Session.owner_id == current_user.id)

    if category:
        query = query.where(Session.category == category)
    if status:
        query = query.where(Session.status == status)

    query = query.order_by(desc(Session.created_at)).offset(skip).limit(limit)
    result = await db.execute(query)
    return result.scalars().all()


@router.get("/{session_id}", response_model=SessionResponse)
async def get_session(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get a specific session by ID."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")
    return session


@router.patch("/{session_id}", response_model=SessionResponse)
async def update_session(
    session_id: uuid.UUID,
    data: SessionUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update session title, notes, or transcript."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    if data.title is not None:
        session.title = data.title
    if data.manual_notes is not None:
        session.manual_notes = data.manual_notes
    if data.transcript is not None:
        session.transcript = data.transcript

    await db.flush()
    return session


@router.post("/{session_id}/start", response_model=SessionResponse)
async def start_recording(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Mark session as recording started."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")
    if session.status not in [SessionStatus.CREATED, SessionStatus.PAUSED]:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Cannot start from status: {session.status}")

    session.status = SessionStatus.RECORDING
    session.started_at = datetime.now(timezone.utc)
    await db.flush()
    return session


@router.post("/{session_id}/stop", response_model=SessionResponse)
async def stop_recording(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Mark session as stopped (ready for audio upload)."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    session.status = SessionStatus.PAUSED
    session.ended_at = datetime.now(timezone.utc)
    await db.flush()
    return session


@router.post("/{session_id}/upload-audio", response_model=SessionResponse)
async def upload_audio(
    session_id: uuid.UUID,
    audio_file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Upload audio file and trigger async transcription + AI analysis."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    # Validate file size
    audio_data = await audio_file.read()
    max_bytes = settings.MAX_AUDIO_SIZE_MB * 1024 * 1024
    if len(audio_data) > max_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Audio file exceeds {settings.MAX_AUDIO_SIZE_MB}MB limit"
        )

    # Validate content type
    allowed_types = {"audio/mpeg", "audio/mp4", "audio/wav", "audio/webm", "audio/ogg", "audio/m4a"}
    content_type = audio_file.content_type or "audio/mpeg"
    if content_type not in allowed_types:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"Unsupported audio format: {content_type}"
        )

    # Upload to storage
    audio_key = storage_service.upload_audio(audio_data, str(session_id), content_type)

    # Update session
    session.audio_file_key = audio_key
    session.audio_size_bytes = len(audio_data)
    session.status = SessionStatus.PROCESSING
    if not session.ended_at:
        session.ended_at = datetime.now(timezone.utc)
    await db.flush()

    # Dispatch Celery task
    transcribe_audio.delay(str(session_id), audio_key, session.language)

    return session


@router.delete("/{session_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_session(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete a session and all associated data."""
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    # Clean up storage
    if session.audio_file_key:
        storage_service.delete_object(settings.MINIO_BUCKET_AUDIO, session.audio_file_key)

    await db.delete(session)
