package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Performance Optimization: Index on `startTime` optimizes `ORDER BY startTime DESC` queries in StudyDao,
// eliminating SQLite full table scans and sort overhead when fetching study history.
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
