from pydantic import BaseModel, field_validator
from typing import Optional, List, Any
from datetime import datetime
from uuid import UUID
from app.models.session import SessionCategory, SessionStatus, RecordingMode


class ParticipantSchema(BaseModel):
    name: str
    email: Optional[str] = None


class SessionCreate(BaseModel):
    title: str
    category: SessionCategory = SessionCategory.MEETING
    language: str = "id"
    participants: Optional[List[ParticipantSchema]] = None
    recording_mode: RecordingMode = RecordingMode.BATCH

    @field_validator("language")
    @classmethod
    def validate_language(cls, v: str) -> str:
        allowed = ["id", "en", "jv", "su", "zh", "ar"]
        if v not in allowed:
            raise ValueError(f"Language must be one of {allowed}")
        return v


class SessionUpdate(BaseModel):
    title: Optional[str] = None
    manual_notes: Optional[str] = None
    transcript: Optional[str] = None


class TranscriptSegment(BaseModel):
    speaker: str
    text: str
    start: float
    end: float
    confidence: float


class ActionItem(BaseModel):
    task: str
    assignee: Optional[str] = None
    due_date: Optional[str] = None


class SessionResponse(BaseModel):
    id: UUID
    owner_id: UUID
    title: str
    category: SessionCategory
    language: str
    participants: Optional[List[Any]] = None
    recording_mode: RecordingMode
    status: SessionStatus
    audio_duration_seconds: Optional[int] = None
    summary: Optional[str] = None
    key_points: Optional[List[Any]] = None
    action_items: Optional[List[Any]] = None
    conclusions: Optional[str] = None
    sentiment: Optional[Any] = None
    manual_notes: Optional[str] = None
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class SessionListResponse(BaseModel):
    id: UUID
    title: str
    category: SessionCategory
    status: SessionStatus
    language: str
    audio_duration_seconds: Optional[int] = None
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    created_at: datetime

    model_config = {"from_attributes": True}
