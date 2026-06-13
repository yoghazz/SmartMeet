import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware

from app.core.config import settings
from app.core.database import engine
from app.core.redis import init_redis, close_redis
from app.api.v1.router import api_router
from app.api.v1.endpoints.documents import share_router

logging.basicConfig(
    level=logging.DEBUG if settings.DEBUG else logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting SmartMeet API...")
    await init_redis()
    logger.info("Redis connected")
    yield
    # Shutdown
    await close_redis()
    await engine.dispose()
    logger.info("SmartMeet API shut down")


app = FastAPI(
    title="SmartMeet API",
    version=settings.APP_VERSION,
    description="AI-Powered Meeting Recorder & Report Generator",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.add_middleware(GZipMiddleware, minimum_size=1000)

# Routers
app.include_router(api_router)
app.include_router(share_router)  # /share/{token}


@app.get("/health", tags=["health"])
async def health_check():
    return {
        "status": "ok",
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
    }
