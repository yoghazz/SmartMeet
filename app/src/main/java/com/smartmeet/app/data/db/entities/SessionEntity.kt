package com.smartmeet.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val status: String,
    val language: String,
    val audioDurationSeconds: Int?,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String
)
