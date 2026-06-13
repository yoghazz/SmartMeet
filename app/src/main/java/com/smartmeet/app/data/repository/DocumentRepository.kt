package com.smartmeet.app.data.repository

import com.smartmeet.app.data.api.SmartMeetApi
import com.smartmeet.app.data.api.models.DocumentResponse
import com.smartmeet.app.data.api.models.GenerateDocRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val api: SmartMeetApi
) {
    suspend fun getDocuments(sessionId: String): List<DocumentResponse> = api.getDocuments(sessionId)

    suspend fun generate(sessionId: String, formats: List<String>): List<DocumentResponse> {
        return api.generateDocuments(sessionId, GenerateDocRequest(formats = formats))
    }
}
