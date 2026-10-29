package com.pocketai.assistant.domain.chat

import com.pocketai.assistant.domain.inference.GenerationEvent
import com.pocketai.assistant.domain.model.AttachmentItem
import com.pocketai.assistant.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesFlow(): Flow<List<ChatMessage>>
    suspend fun getMessagesSnapshot(): List<ChatMessage>
    suspend fun sendUserMessage(
        content: String,
        attachments: List<AttachmentItem>
    ): Flow<GenerationEvent>
    suspend fun stopGeneration()
    suspend fun retryLastAssistantResponse(): Flow<GenerationEvent>?
    suspend fun deleteMessage(id: String)
    suspend fun clearHistory()
    suspend fun getMessageCount(): Int
}

