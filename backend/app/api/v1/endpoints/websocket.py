import uuid
import json
import logging
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import AsyncSessionLocal
from app.core.security import decode_token
from app.models.session import Session, SessionStatus
from app.models.user import User
from app.services.asr_service import asr_service

logger = logging.getLogger(__name__)

router = APIRouter(tags=["websocket"])


@router.websocket("/ws/sessions/{session_id}/stream")
async def audio_stream(
    websocket: WebSocket,
    session_id: uuid.UUID,
    token: str = Query(...),
):
    """
    WebSocket endpoint for real-time audio streaming.

    Client sends:
    - Binary frames: raw audio chunks (PCM 16kHz mono)
    - JSON text: {"type": "ping"} | {"type": "stop"}

    Server sends:
    - {"type": "transcript_chunk", "text": str, "is_final": bool}
    - {"type": "status", "status": str}
    - {"type": "error", "message": str}
    - {"type": "pong"}
    """
    # Validate token
    payload = decode_token(token)
    if not payload or payload.get("type") != "access":
        await websocket.close(code=4001, reason="Unauthorized")
        return

    user_id = payload.get("sub")

    async with AsyncSessionLocal() as db:
        # Validate session ownership
        result = await db.execute(
            select(Session).where(Session.id == session_id)
        )
        session = result.scalar_one_or_none()

        if not session or str(session.owner_id) != user_id:
            await websocket.close(code=4003, reason="Session not found or forbidden")
            return

        if session.status not in [SessionStatus.CREATED, SessionStatus.RECORDING, SessionStatus.PAUSED]:
            await websocket.close(code=4004, reason="Session not in recordable state")
            return

        # Mark as recording
        session.status = SessionStatus.RECORDING
        await db.commit()

    await websocket.accept()
    await websocket.send_text(json.dumps({"type": "status", "status": "connected"}))

    audio_buffer = bytearray()
    chunk_size = 16000 * 2 * 5  # 5 seconds of PCM 16kHz 16-bit mono

    try:
        while True:
            try:
                data = await websocket.receive()
            except WebSocketDisconnect:
                break

            if "bytes" in data:
                audio_buffer.extend(data["bytes"])

                # Process when buffer has enough data
                if len(audio_buffer) >= chunk_size:
                    chunk = bytes(audio_buffer[:chunk_size])
                    audio_buffer = audio_buffer[chunk_size:]

                    try:
                        # Run ASR on chunk (synchronous in thread pool)
                        import asyncio
                        text = await asyncio.get_event_loop().run_in_executor(
                            None,
                            lambda: asr_service.transcribe_chunk(chunk, language="id")
                        )
                        if text:
                            await websocket.send_text(json.dumps({
                                "type": "transcript_chunk",
                                "text": text,
                                "is_final": False,
                            }))
                    except Exception as e:
                        logger.warning(f"Chunk transcription error: {e}")

            elif "text" in data:
                try:
                    msg = json.loads(data["text"])
                except json.JSONDecodeError:
                    continue

                if msg.get("type") == "ping":
                    await websocket.send_text(json.dumps({"type": "pong"}))

                elif msg.get("type") == "stop":
                    # Process remaining buffer
                    if audio_buffer:
                        try:
                            import asyncio
                            text = await asyncio.get_event_loop().run_in_executor(
                                None,
                                lambda: asr_service.transcribe_chunk(bytes(audio_buffer), language="id")
                            )
                            if text:
                                await websocket.send_text(json.dumps({
                                    "type": "transcript_chunk",
                                    "text": text,
                                    "is_final": True,
                                }))
                        except Exception as e:
                            logger.warning(f"Final chunk error: {e}")

                    await websocket.send_text(json.dumps({"type": "status", "status": "stopped"}))
                    break

    except WebSocketDisconnect:
        logger.info(f"WebSocket disconnected for session {session_id}")
    except Exception as e:
        logger.error(f"WebSocket error for session {session_id}: {e}")
        try:
            await websocket.send_text(json.dumps({"type": "error", "message": str(e)}))
        except Exception:
            pass
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
