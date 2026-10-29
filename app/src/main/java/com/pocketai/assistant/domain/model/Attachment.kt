package com.pocketai.assistant.domain.model

enum class AttachmentType {
    IMAGE,
    PDF,
    DOCX,
    XLSX,
    PPTX,
    TEXT,
    CSV,
    MARKDOWN,
    VIDEO,
    UNKNOWN
}

enum class AttachmentStatus {
    PENDING,
    PROCESSING,
    READY,
    ERROR
}

data class AttachmentItem(
    val id: String,
    val uriString: String,
    val displayName: String,
    val type: AttachmentType,
    val mimeType: String,
    val sizeBytes: Long,
    val status: AttachmentStatus = AttachmentStatus.PENDING,
    val extractedText: String? = null,
    val pageOrItemCount: Int? = null,
    val durationMs: Long? = null,
    val errorMessage: String? = null
)

