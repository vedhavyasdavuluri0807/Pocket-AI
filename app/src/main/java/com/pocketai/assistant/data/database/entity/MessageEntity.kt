package com.pocketai.assistant.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index("timestamp")
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val status: String,
    val errorMessage: String? = null,
    val attachmentsJson: String = "[]"
)

