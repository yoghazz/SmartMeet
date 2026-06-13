from celery import Celery
from app.core.config import settings

celery_app = Celery(
    "smartmeet",
    broker=settings.CELERY_BROKER_URL,
    backend=settings.CELERY_RESULT_BACKEND,
    include=["app.workers.tasks"],
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="Asia/Jakarta",
    enable_utc=True,
    task_track_started=True,
    task_acks_late=True,
    worker_prefetch_multiplier=1,  # Fair distribution for long-running tasks
    task_routes={
        "app.workers.tasks.transcribe_audio": {"queue": "asr"},
        "app.workers.tasks.analyze_transcript": {"queue": "nlp"},
        "app.workers.tasks.generate_documents": {"queue": "docgen"},
    },
)
