from fastapi import APIRouter
from app.api.v1.endpoints import auth, sessions, documents, websocket

api_router = APIRouter(prefix="/api/v1")

api_router.include_router(auth.router)
api_router.include_router(sessions.router)
api_router.include_router(documents.router)
api_router.include_router(websocket.router)

# Public share endpoint (no /api/v1 prefix)
from app.api.v1.endpoints.documents import share_router
