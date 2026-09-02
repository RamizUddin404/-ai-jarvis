package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Performance Optimization: Index on `timestamp` optimizes `ORDER BY timestamp ASC` queries in ChatDao,
// preventing full table scans and temporary sorting overhead during reactive Flow emissions.
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
