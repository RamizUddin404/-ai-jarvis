package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Optimization: Index on timestamp speeds up sorting queries (ORDER BY timestamp ASC)
// avoiding expensive full-table table scans during chat history retrieval.
@Entity(
    tableName = "chat_history",
    indices = [Index(value = ["timestamp"])]
)
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "jarvis"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
