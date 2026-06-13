import uuid
from datetime import datetime
from sqlalchemy import String, Integer, Float, ForeignKey, DateTime, Enum as SAEnum, Text, JSON, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID
from app.core.database import Base
import enum


class SessionCategory(str, enum.Enum):
    MEETING = "meeting"
    SEMINAR = "seminar"
    LECTURE = "lecture"
    DISCUSSION = "discussion"
    INTERVIEW = "interview"
    OTHER = "other"


class SessionStatus(str, enum.Enum):
    CREATED = "created"
    RECORDING = "recording"
    PAUSED = "paused"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class RecordingMode(str, enum.Enum):
    REALTIME = "realtime"
    BATCH = "batch"
    HYBRID = "hybrid"


class Session(Base):
    __tablename__ = "sessions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )

    title: Mapped[str] = mapped_column(String(500), nullable=False)
    category: Mapped[SessionCategory] = mapped_column(
        SAEnum(SessionCategory), default=SessionCategory.MEETING, nullable=False
    )
    language: Mapped[str] = mapped_column(String(10), default="id", nullable=False)  # id, en
    participants: Mapped[list | None] = mapped_column(JSON, nullable=True)  # [{name, email}]
    recording_mode: Mapped[RecordingMode] = mapped_column(
        SAEnum(RecordingMode), default=RecordingMode.BATCH, nullable=False
    )
    status: Mapped[SessionStatus] = mapped_column(
        SAEnum(SessionStatus), default=SessionStatus.CREATED, nullable=False, index=True
    )

    # Audio
    audio_file_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    audio_duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    audio_size_bytes: Mapped[int | None] = mapped_column(Integer, nullable=True)

    # Processing results
    transcript: Mapped[str | None] = mapped_column(Text, nullable=True)
    transcript_segments: Mapped[list | None] = mapped_column(JSON, nullable=True)  # [{speaker, text, start, end, confidence}]
    summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    key_points: Mapped[list | None] = mapped_column(JSON, nullable=True)
    action_items: Mapped[list | None] = mapped_column(JSON, nullable=True)  # [{task, assignee, due_date}]
    conclusions: Mapped[str | None] = mapped_column(Text, nullable=True)
    sentiment: Mapped[dict | None] = mapped_column(JSON, nullable=True)

    # Manual notes added during recording
    manual_notes: Mapped[str | None] = mapped_column(Text, nullable=True)

    # Timestamps
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    owner: Mapped["User"] = relationship("User", back_populates="sessions")
    documents: Mapped[list["Document"]] = relationship("Document", back_populates="session", lazy="select")

    def __repr__(self) -> str:
        return f"<Session {self.title} [{self.status}]>"
