package com.pocketai.assistant.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String = DEFAULT_CONVERSATION_ID,
    val title: String = "Assistant Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_CONVERSATION_ID = "main_single_conversation"
    }
}

