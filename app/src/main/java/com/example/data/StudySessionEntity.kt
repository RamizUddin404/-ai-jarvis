package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val sessionNotes: String = "",
    val aiSummary: String = "",
    val isSummarySaved: Boolean = false
)
