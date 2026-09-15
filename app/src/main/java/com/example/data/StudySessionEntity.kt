package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Optimization: Index on startTime speeds up sorting queries (ORDER BY startTime DESC)
// avoiding expensive full-table table scans when fetching study session history.
@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["startTime"])]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val sessionNotes: String = "",
    val aiSummary: String = "",
    val isSummarySaved: Boolean = false
)
