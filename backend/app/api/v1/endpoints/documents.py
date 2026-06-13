import uuid
import secrets
from typing import List
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import get_db
from app.models.user import User
from app.models.session import Session, SessionStatus
from app.models.document import Document, DocumentFormat, DocumentStatus
from app.schemas.document import DocumentGenerateRequest, DocumentResponse, ShareLinkResponse
from app.api.deps import get_current_user
from app.workers.tasks import generate_documents

router = APIRouter(prefix="/sessions/{session_id}/documents", tags=["documents"])


@router.post("", response_model=List[DocumentResponse], status_code=status.HTTP_202_ACCEPTED)
async def request_documents(
    session_id: uuid.UUID,
    data: DocumentGenerateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Request document generation for a completed session.
    Returns document records immediately; generation happens async.
    """
    result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    session = result.scalar_one_or_none()
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    if session.status != SessionStatus.COMPLETED:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Session must be completed before generating documents. Current status: {session.status}"
        )

    # Enforce free tier: PDF only
    from app.models.user import SubscriptionPlan
    if current_user.subscription_plan == SubscriptionPlan.FREE:
        data.formats = [f for f in data.formats if f == DocumentFormat.PDF]
        if not data.formats:
            data.formats = [DocumentFormat.PDF]

    # Create document records
    docs = []
    for fmt in data.formats:
        doc = Document(
            session_id=session_id,
            format=fmt,
            theme=data.theme,
            status=DocumentStatus.PENDING,
        )
        db.add(doc)
        docs.append(doc)

    await db.flush()

    # Dispatch generation task
    doc_ids = [str(d.id) for d in docs]
    generate_documents.delay(str(session_id), doc_ids)

    return docs


@router.get("", response_model=List[DocumentResponse])
async def list_documents(
    session_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List all documents for a session."""
    # Verify session ownership
    sess_result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    if not sess_result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    result = await db.execute(select(Document).where(Document.session_id == session_id))
    return result.scalars().all()


@router.get("/{document_id}", response_model=DocumentResponse)
async def get_document(
    session_id: uuid.UUID,
    document_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get document status and download URL."""
    sess_result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    if not sess_result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    result = await db.execute(
        select(Document).where(Document.id == document_id, Document.session_id == session_id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Document not found")

    # Refresh presigned URL if completed
    if doc.status == DocumentStatus.COMPLETED and doc.file_key:
        from app.services.storage_service import storage_service
        doc.download_url = storage_service.get_document_url(doc.file_key, expires_seconds=3600)

    return doc


@router.post("/{document_id}/share", response_model=ShareLinkResponse)
async def create_share_link(
    session_id: uuid.UUID,
    document_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a 7-day shareable read-only link for the document."""
    sess_result = await db.execute(
        select(Session).where(Session.id == session_id, Session.owner_id == current_user.id)
    )
    if not sess_result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    result = await db.execute(
        select(Document).where(Document.id == document_id, Document.session_id == session_id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Document not found")

    if doc.status != DocumentStatus.COMPLETED:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Document not ready")

    token = secrets.token_urlsafe(32)
    expires = datetime.now(timezone.utc) + timedelta(days=7)

    doc.share_token = token
    doc.share_expires_at = expires
    await db.flush()

    return ShareLinkResponse(
        share_url=f"/share/{token}",
        expires_at=expires,
    )


# Public share endpoint (no auth)
share_router = APIRouter(prefix="/share", tags=["share"])


@share_router.get("/{token}", response_model=DocumentResponse)
async def get_shared_document(token: str, db: AsyncSession = Depends(get_db)):
    """Access a shared document via token (no login required)."""
    result = await db.execute(
        select(Document).where(Document.share_token == token)
    )
    doc = result.scalar_one_or_none()

    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Share link not found")

    if doc.share_expires_at and doc.share_expires_at < datetime.now(timezone.utc):
        raise HTTPException(status_code=status.HTTP_410_GONE, detail="Share link has expired")

    # Refresh presigned URL
    if doc.file_key:
        from app.services.storage_service import storage_service
        doc.download_url = storage_service.get_document_url(doc.file_key, expires_seconds=3600)

    return doc
