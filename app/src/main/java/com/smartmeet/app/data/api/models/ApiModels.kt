package com.smartmeet.app.data.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val password: String
)

data class LoginRequest(val email: String, val password: String)
data class RefreshTokenRequest(@SerializedName("refresh_token") val refreshToken: String)
data class GoogleAuthRequest(@SerializedName("id_token") val idToken: String)

data class CreateSessionRequest(
    val title: String,
    val category: String,
    val language: String,
    val participants: List<Participant>?,
    @SerializedName("recording_mode") val recordingMode: String
)

data class UpdateSessionRequest(
    val title: String? = null,
    @SerializedName("manual_notes") val manualNotes: String? = null,
    val transcript: String? = null
)

data class GenerateDocRequest(
    val formats: List<String>,
    val theme: String = "professional"
)

data class Participant(val name: String, val email: String? = null)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class UserResponse(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("subscription_plan") val subscriptionPlan: String,
    @SerializedName("auth_provider") val authProvider: String,
    @SerializedName("created_at") val createdAt: String
)

data class SessionListResponse(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val language: String,
    @SerializedName("audio_duration_seconds") val audioDurationSeconds: Int?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("ended_at") val endedAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class SessionResponse(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    val title: String,
    val category: String,
    val language: String,
    val participants: List<Participant>?,
    @SerializedName("recording_mode") val recordingMode: String,
    val status: String,
    @SerializedName("audio_duration_seconds") val audioDurationSeconds: Int?,
    val summary: String?,
    @SerializedName("key_points") val keyPoints: List<String>?,
    @SerializedName("action_items") val actionItems: List<ActionItem>?,
    val conclusions: String?,
    val sentiment: SentimentData?,
    @SerializedName("manual_notes") val manualNotes: String?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("ended_at") val endedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ActionItem(
    val task: String,
    val assignee: String?,
    @SerializedName("due_date") val dueDate: String?
)

data class SentimentData(
    val overall: String,
    val score: Float,
    val topics: Map<String, String>?
)

data class DocumentResponse(
    val id: String,
    @SerializedName("session_id") val sessionId: String,
    val format: String,
    val status: String,
    val theme: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Int?,
    @SerializedName("download_url") val downloadUrl: String?,
    @SerializedName("share_token") val shareToken: String?,
    @SerializedName("share_expires_at") val shareExpiresAt: String?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("created_at") val createdAt: String
)

data class ShareLinkResponse(
    @SerializedName("share_url") val shareUrl: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class TranscriptChunk(val text: String, val isFinal: Boolean)

data class WebSocketMessage(
    val type: String,
    val text: String? = null,
    @SerializedName("is_final") val isFinal: Boolean = false,
    val status: String? = null
)
