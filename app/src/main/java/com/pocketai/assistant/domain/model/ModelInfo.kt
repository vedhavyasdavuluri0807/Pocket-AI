package com.pocketai.assistant.domain.model

enum class ModelFormat {
    GGUF,
    UNKNOWN
}

enum class ModelLoadStatus {
    NOT_LOADED,
    VALIDATING,
    LOADING,
    LOADED,
    FAILED
}

data class ModelInfo(
    val uriString: String,
    val displayName: String,
    val format: ModelFormat,
    val fileSizeBytes: Long,
    val architecture: String = "unknown",
    val contextLength: Int = 2048,
    val tensorCount: Long = 0,
    val kvCount: Long = 0,
    val isAccessible: Boolean = true,
    val loadStatus: ModelLoadStatus = ModelLoadStatus.NOT_LOADED,
    val lastError: String? = null
)

