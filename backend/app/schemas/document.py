from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from uuid import UUID
from app.models.document import DocumentFormat, DocumentStatus


class DocumentGenerateRequest(BaseModel):
    formats: List[DocumentFormat]
    theme: str = "professional"  # professional, modern, minimalis


class DocumentResponse(BaseModel):
    id: UUID
    session_id: UUID
    format: DocumentFormat
    status: DocumentStatus
    theme: str
    file_size_bytes: Optional[int] = None
    download_url: Optional[str] = None
    share_token: Optional[str] = None
    share_expires_at: Optional[datetime] = None
    error_message: Optional[str] = None
    created_at: datetime

    model_config = {"from_attributes": True}


class ShareLinkResponse(BaseModel):
    share_url: str
    expires_at: datetime
