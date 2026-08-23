package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    suspend fun insertChat(chat: ChatEntity) {
        chatDao.insertChat(chat)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
