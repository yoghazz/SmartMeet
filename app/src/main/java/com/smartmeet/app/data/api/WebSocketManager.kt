package com.smartmeet.app.data.api

import com.google.gson.Gson
import com.smartmeet.app.data.api.models.TranscriptChunk
import com.smartmeet.app.data.api.models.WebSocketMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString.Companion.toByteString

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    val transcriptFlow = MutableSharedFlow<TranscriptChunk>(extraBufferCapacity = 64)
    val statusFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)

    fun connect(sessionId: String, token: String) {
        val url = "wss://api.sityreq.online/ws/sessions/$sessionId/stream?token=$token"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                statusFlow.tryEmit("connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = Gson().fromJson(text, WebSocketMessage::class.java)
                when (msg.type) {
                    "transcript_chunk" -> transcriptFlow.tryEmit(TranscriptChunk(msg.text.orEmpty(), msg.isFinal))
                    "status" -> statusFlow.tryEmit(msg.status.orEmpty())
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                statusFlow.tryEmit(t.message ?: "failure")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                statusFlow.tryEmit("closed:$reason")
            }
        })
    }

    fun sendAudioChunk(pcmData: ByteArray) {
        webSocket?.send(pcmData.toByteString())
    }

    fun sendStop() {
        webSocket?.send("{\"type\":\"stop\"}")
    }

    fun disconnect() {
        webSocket?.close(1000, "Recording stopped")
        webSocket = null
    }
}
