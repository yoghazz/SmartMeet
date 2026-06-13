import io
import uuid
from typing import Optional
from minio import Minio
from minio.error import S3Error
from app.core.config import settings


class StorageService:
    def __init__(self):
        self.client = Minio(
            endpoint=settings.MINIO_ENDPOINT,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE,
        )
        self._ensure_buckets()

    def _ensure_buckets(self):
        for bucket in [settings.MINIO_BUCKET_AUDIO, settings.MINIO_BUCKET_DOCS]:
            if not self.client.bucket_exists(bucket):
                self.client.make_bucket(bucket)

    def upload_audio(self, data: bytes, session_id: str, content_type: str = "audio/mpeg") -> str:
        """Upload audio file, return object key."""
        key = f"{session_id}/audio_{uuid.uuid4().hex}.m4a"
        self.client.put_object(
            bucket_name=settings.MINIO_BUCKET_AUDIO,
            object_name=key,
            data=io.BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        return key

    def upload_document(self, data: bytes, session_id: str, fmt: str) -> str:
        """Upload generated document, return object key."""
        ext_map = {"pdf": "pdf", "docx": "docx", "pptx": "pptx"}
        ext = ext_map.get(fmt, fmt)
        key = f"{session_id}/report_{uuid.uuid4().hex}.{ext}"
        content_types = {
            "pdf": "application/pdf",
            "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        }
        self.client.put_object(
            bucket_name=settings.MINIO_BUCKET_DOCS,
            object_name=key,
            data=io.BytesIO(data),
            length=len(data),
            content_type=content_types.get(fmt, "application/octet-stream"),
        )
        return key

    def get_presigned_url(self, bucket: str, key: str, expires_seconds: int = 3600) -> str:
        """Generate presigned download URL."""
        from datetime import timedelta
        return self.client.presigned_get_object(
            bucket_name=bucket,
            object_name=key,
            expires=timedelta(seconds=expires_seconds),
        )

    def get_audio_url(self, key: str, expires_seconds: int = 3600) -> str:
        return self.get_presigned_url(settings.MINIO_BUCKET_AUDIO, key, expires_seconds)

    def get_document_url(self, key: str, expires_seconds: int = 3600) -> str:
        return self.get_presigned_url(settings.MINIO_BUCKET_DOCS, key, expires_seconds)

    def download_audio(self, key: str) -> bytes:
        """Download audio for processing."""
        response = self.client.get_object(settings.MINIO_BUCKET_AUDIO, key)
        data = response.read()
        response.close()
        response.release_conn()
        return data

    def delete_object(self, bucket: str, key: str):
        try:
            self.client.remove_object(bucket, key)
        except S3Error:
            pass


storage_service = StorageService()
