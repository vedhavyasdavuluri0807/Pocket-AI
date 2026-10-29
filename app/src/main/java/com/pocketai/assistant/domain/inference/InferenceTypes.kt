package com.pocketai.assistant.domain.inference

import kotlinx.coroutines.flow.Flow

data class ModelConfig(
    val uriString: String,
    val displayName: String,
    val contextLength: Int = 2048,
    val threadCount: Int = 4,
    val useGpu: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512
)

data class GenerationRequest(
    val prompt: String,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val stopSequences: List<String> = listOf("User:", "Human:", "Assistant:", "<|im_end|>", "</s>")
)

sealed class GenerationEvent {
    data class Token(val text: String) : GenerationEvent()
    data class Completed(val fullText: String, val tokensGenerated: Int, val durationMs: Long) : GenerationEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : GenerationEvent()
    object Interrupted : GenerationEvent()
}

sealed class ModelRuntimeState {
    object Unloaded : ModelRuntimeState()
    data class Loading(val progress: Float = 0f, val message: String = "Loading model...") : ModelRuntimeState()
    data class Loaded(val config: ModelConfig, val architecture: String, val ramUsageBytes: Long = 0) : ModelRuntimeState()
    data class Generating(val prompt: String) : ModelRuntimeState()
    data class Error(val message: String, val canRetry: Boolean = true) : ModelRuntimeState()
}

interface LocalModelEngine {
    val state: Flow<ModelRuntimeState>
    val isAvailable: Boolean
    val availabilityReason: String

    suspend fun loadModel(model: ModelConfig)
    fun generate(request: GenerationRequest): Flow<GenerationEvent>
    suspend fun stopGeneration()
    suspend fun unloadModel()
}

