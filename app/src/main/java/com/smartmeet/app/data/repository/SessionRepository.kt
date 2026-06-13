package com.smartmeet.app.data.repository

import com.smartmeet.app.data.api.SmartMeetApi
import com.smartmeet.app.data.api.models.CreateSessionRequest
import com.smartmeet.app.data.api.models.Participant
import com.smartmeet.app.data.api.models.SessionResponse
import com.smartmeet.app.data.db.dao.SessionDao
import com.smartmeet.app.data.db.entities.SessionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SessionRepository @Inject constructor(
    private val api: SmartMeetApi,
    private val dao: SessionDao
) {
    fun observeCachedSessions(): Flow<List<SessionEntity>> = dao.observeAll()

    suspend fun refreshSessions() {
        val remote = api.getSessions()
        dao.upsertAll(remote.map {
            SessionEntity(
                id = it.id,
                title = it.title,
                category = it.category,
                status = it.status,
                language = it.language,
                audioDurationSeconds = it.audioDurationSeconds,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                createdAt = it.createdAt
            )
        })
    }

    suspend fun createSession(
        title: String,
        category: String,
        language: String,
        recordingMode: String,
        participants: List<Participant>
    ): SessionResponse {
        val response = api.createSession(
            CreateSessionRequest(
                title = title,
                category = category,
                language = language,
                participants = participants,
                recordingMode = recordingMode
            )
        )
        dao.upsert(
            SessionEntity(
                id = response.id,
                title = response.title,
                category = response.category,
                status = response.status,
                language = response.language,
                audioDurationSeconds = response.audioDurationSeconds,
                startedAt = response.startedAt,
                endedAt = response.endedAt,
                createdAt = response.createdAt
            )
        )
        return response
    }

    suspend fun getSession(id: String): SessionResponse = api.getSession(id)
    suspend fun startRecording(id: String): SessionResponse = api.startRecording(id)
    suspend fun stopRecording(id: String): SessionResponse = api.stopRecording(id)
    suspend fun uploadAudio(id: String, part: okhttp3.MultipartBody.Part): SessionResponse = api.uploadAudio(id, part)
}
