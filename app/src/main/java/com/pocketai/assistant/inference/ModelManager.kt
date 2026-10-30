package com.pocketai.assistant.inference

import android.content.Context
import android.net.Uri
import com.pocketai.assistant.data.preferences.PreferencesManager
import com.pocketai.assistant.domain.inference.LocalModelEngine
import com.pocketai.assistant.domain.inference.ModelConfig
import com.pocketai.assistant.domain.inference.ModelRuntimeState
import com.pocketai.assistant.domain.model.ModelInfo
import com.pocketai.assistant.domain.model.ModelLoadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelManager(
    private val context: Context,
    val engine: LocalModelEngine,
    private val preferencesManager: PreferencesManager,
    private val applicationScope: CoroutineScope
) {
    private val _currentModelInfo = MutableStateFlow<ModelInfo?>(null)
    val currentModelInfo: StateFlow<ModelInfo?> = _currentModelInfo.asStateFlow()

    init {
        // Continuously sync model load status with native engine state
        applicationScope.launch {
            engine.state.collect { runtimeState ->
                val current = _currentModelInfo.value ?: return@collect
                when (runtimeState) {
                    is ModelRuntimeState.Loaded -> {
                        if (current.loadStatus != ModelLoadStatus.LOADED) {
                            _currentModelInfo.value = current.copy(
                                loadStatus = ModelLoadStatus.LOADED,
                                lastError = null
                            )
                        }
                    }
                    is ModelRuntimeState.Loading -> {
                        if (current.loadStatus != ModelLoadStatus.LOADING) {
                            _currentModelInfo.value = current.copy(
                                loadStatus = ModelLoadStatus.LOADING,
                                lastError = null
                            )
                        }
                    }
                    is ModelRuntimeState.Error -> {
                        _currentModelInfo.value = current.copy(
                            loadStatus = ModelLoadStatus.FAILED,
                            lastError = runtimeState.message
                        )
                    }
                    is ModelRuntimeState.Unloaded -> {
                        if (current.loadStatus == ModelLoadStatus.LOADED || current.loadStatus == ModelLoadStatus.LOADING) {
                            _currentModelInfo.value = current.copy(
                                loadStatus = ModelLoadStatus.NOT_LOADED
                            )
                        }
                    }
                    is ModelRuntimeState.Generating -> {
                        // Remain in LOADED state during active generation
                    }
                }
            }
        }

        // Restore active model if saved in preferences
        applicationScope.launch {
            val prefs = preferencesManager.userPreferencesFlow.first()
            prefs.activeModelUri?.let { uriStr ->
                inspectAndSetModel(Uri.parse(uriStr))
            }
        }
    }

    suspend fun inspectAndSetModel(uri: Uri): ModelInfo? = withContext(Dispatchers.IO) {
        // If an existing model was loaded, unload it before switching
        if (_currentModelInfo.value?.loadStatus == ModelLoadStatus.LOADED) {
            try {
                engine.unloadModel()
            } catch (_: Exception) {}
        }

        val resolver = context.contentResolver
        var displayName = uri.lastPathSegment ?: "model.gguf"
        var fileSize = -1L

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}

        if (fileSize <= 0) {
            try {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        val result = GgufValidator.inspectModelUri(context, uri, displayName, fileSize)
        when (result) {
            is GgufValidator.ValidationResult.Success -> {
                val info = result.modelInfo.copy(loadStatus = ModelLoadStatus.NOT_LOADED, lastError = null)
                _currentModelInfo.value = info
                preferencesManager.setActiveModelUri(uri.toString())
                info
            }
            is GgufValidator.ValidationResult.Incompatible -> {
                val info = ModelInfo(
                    uriString = uri.toString(),
                    displayName = displayName,
                    format = com.pocketai.assistant.domain.model.ModelFormat.UNKNOWN,
                    fileSizeBytes = fileSize,
                    loadStatus = ModelLoadStatus.FAILED,
                    lastError = result.reason
                )
                _currentModelInfo.value = info
                info
            }
            is GgufValidator.ValidationResult.Error -> {
                val info = ModelInfo(
                    uriString = uri.toString(),
                    displayName = displayName,
                    format = com.pocketai.assistant.domain.model.ModelFormat.UNKNOWN,
                    fileSizeBytes = fileSize,
                    loadStatus = ModelLoadStatus.FAILED,
                    lastError = result.message
                )
                _currentModelInfo.value = info
                info
            }
        }
    }

    suspend fun loadCurrentModel(): Result<Unit> = withContext(Dispatchers.IO) {
        val info = _currentModelInfo.value
            ?: return@withContext Result.failure(IllegalStateException("No model selected. Please choose a GGUF file in Model Management."))

        try {
            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.LOADING, lastError = null)

            val prefs = preferencesManager.userPreferencesFlow.first()
            val config = ModelConfig(
                uriString = info.uriString,
                displayName = info.displayName,
                contextLength = minOf(info.contextLength, 4096),
                threadCount = prefs.threadCount,
                maxTokens = prefs.maxTokens,
                temperature = prefs.temperature
            )

            engine.loadModel(config)

            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.LOADED, lastError = null)
            Result.success(Unit)
        } catch (e: Exception) {
            val errMessage = e.localizedMessage ?: "Failed to initialize model in native engine."
            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.FAILED, lastError = errMessage)
            Result.failure(e)
        }
    }

    suspend fun unloadCurrentModel() = withContext(Dispatchers.IO) {
        try {
            engine.unloadModel()
        } catch (_: Exception) {}
        _currentModelInfo.value = _currentModelInfo.value?.copy(
            loadStatus = ModelLoadStatus.NOT_LOADED,
            lastError = null
        )
    }

    suspend fun removeModelReference() = withContext(Dispatchers.IO) {
        unloadCurrentModel()
        _currentModelInfo.value = null
        preferencesManager.setActiveModelUri(null)
    }
}
