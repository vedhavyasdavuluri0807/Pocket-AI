package com.pocketai.assistant.domain.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class MessageStatus {
    SENDING,
    STREAMING,
    SUCCESS,
    ERROR,
    CANCELLED
}

data class AttachmentRef(
    val id: String,
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val extractedText: String? = null
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
    val errorMessage: String? = null,
    val attachments: List<AttachmentRef> = emptyList()
)

