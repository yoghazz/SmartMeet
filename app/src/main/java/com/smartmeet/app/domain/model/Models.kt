package com.smartmeet.app.domain.model

enum class SessionStatus {
    CREATED, RECORDING, PAUSED, PROCESSING, COMPLETED, FAILED
}

data class SessionSummary(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val language: String,
    val durationSeconds: Int? = null,
    val createdAt: String,
    val startedAt: String? = null,
    val endedAt: String? = null
)

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val subscriptionPlan: String
)
