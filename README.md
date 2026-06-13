# SmartMeet — AI-Powered Meeting Recorder & Report Generator

Platform Android + Server untuk merekam rapat dan menghasilkan laporan otomatis menggunakan AI.

---

## Tech Stack

| Layer | Teknologi |
|---|---|
| API | FastAPI (Python 3.11) |
| Database | PostgreSQL 16 + Redis 7 |
| Queue | Celery + Redis |
| ASR | OpenAI Whisper |
| NLP | OpenAI GPT-4o |
| Doc Generation | reportlab, python-docx, python-pptx |
| Storage | MinIO (S3-compatible) |
| Client | Android (Jetpack Compose) |

---

## Struktur Proyek

```
smartmeet/
├── backend/
│   ├── app/
│   │   ├── api/v1/endpoints/   # auth, sessions, documents, websocket
│   │   ├── core/               # config, database, redis, security
│   │   ├── models/             # SQLAlchemy ORM models
│   │   ├── schemas/            # Pydantic schemas
│   │   ├── services/           # asr, nlp, document, storage, auth
│   │   ├── workers/            # Celery tasks
│   │   └── main.py
│   ├── alembic/                # DB migrations
│   ├── Dockerfile
│   ├── requirements.txt
│   └── .env.example
├── android/                    # Android client (Jetpack Compose)
├── docker-compose.yml
└── README.md
```

---

## Quick Start (Docker)

### 1. Setup environment

```bash
cd backend
cp .env.example .env
# Edit .env: isi OPENAI_API_KEY, SECRET_KEY, JWT_SECRET_KEY
```

### 2. Jalankan semua services

```bash
docker-compose up -d
```

Services yang berjalan:
- **API**: http://localhost:8000 | Docs: http://localhost:8000/docs
- **MinIO Console**: http://localhost:9001 (minioadmin / minioadmin)
- **Celery Flower**: http://localhost:5555
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### 3. Cek health

```bash
curl http://localhost:8000/health
```

---

## Development (tanpa Docker)

### Prerequisites
- Python 3.11+
- PostgreSQL running di localhost:5432
- Redis running di localhost:6379
- MinIO running di localhost:9000

```bash
cd backend

# Virtual environment
python -m venv .venv
.venv\Scripts\activate      # Windows
# source .venv/bin/activate  # Linux/Mac

# Install dependencies
pip install -r requirements.txt

# Setup .env
cp .env.example .env
# Edit .env sesuai kebutuhan

# Run migrations
alembic upgrade head

# Jalankan API
uvicorn app.main:app --reload --port 8000

# Jalankan Celery worker (terminal terpisah)
celery -A app.workers.celery_app worker -Q asr,nlp,docgen -c 2 --loglevel=info
```

---

## API Endpoints

### Auth
| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/v1/auth/register` | Daftar akun baru |
| POST | `/api/v1/auth/login` | Login email/password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/google` | Login via Google |
| GET | `/api/v1/auth/me` | Info user saat ini |

### Sessions
| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/v1/sessions` | Buat sesi baru |
| GET | `/api/v1/sessions` | List semua sesi |
| GET | `/api/v1/sessions/{id}` | Detail sesi |
| PATCH | `/api/v1/sessions/{id}` | Update sesi |
| POST | `/api/v1/sessions/{id}/start` | Mulai rekaman |
| POST | `/api/v1/sessions/{id}/stop` | Stop rekaman |
| POST | `/api/v1/sessions/{id}/upload-audio` | Upload audio → trigger ASR+AI |
| DELETE | `/api/v1/sessions/{id}` | Hapus sesi |

### Documents
| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/v1/sessions/{id}/documents` | Generate PDF/DOCX/PPTX |
| GET | `/api/v1/sessions/{id}/documents` | List dokumen |
| GET | `/api/v1/sessions/{id}/documents/{doc_id}` | Status + download URL |
| POST | `/api/v1/sessions/{id}/documents/{doc_id}/share` | Buat share link (7 hari) |
| GET | `/share/{token}` | Akses dokumen via share link |

### WebSocket
```
WS /ws/sessions/{session_id}/stream?token=<JWT>
```
Real-time audio streaming + live transcription.

---

## Alur Utama

```
1. POST /auth/register atau /auth/login → dapat JWT token
2. POST /sessions → buat sesi baru
3. POST /sessions/{id}/start → tandai mulai rekam
4. [Android stream audio via WebSocket ATAU upload file setelah selesai]
5. POST /sessions/{id}/upload-audio → Celery: ASR → NLP → COMPLETED
6. POST /sessions/{id}/documents → generate PDF/DOCX/PPTX (async)
7. GET /sessions/{id}/documents/{doc_id} → polling status, ambil download URL
```

---

## Free vs Pro

| Fitur | Free | Pro |
|---|---|---|
| Format output | PDF only | PDF + DOCX + PPTX |
| Rekaman/bulan | 3 jam | Unlimited |

---

## Keamanan

- JWT access token (30 menit) + refresh token (7 hari)
- Semua endpoint butuh Bearer token kecuali `/share/{token}` dan `/health`
- Audio files divalidasi content-type dan ukuran (max 500MB)
- Non-root user di Docker container
