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
        applicationScope.launch {
            // Restore active model if saved in preferences
            val prefs = preferencesManager.userPreferencesFlow.first()
            prefs.activeModelUri?.let { uriStr ->
                inspectAndSetModel(Uri.parse(uriStr))
            }
        }
    }

    suspend fun inspectAndSetModel(uri: Uri): ModelInfo? = withContext(Dispatchers.IO) {
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
                val info = result.modelInfo
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
        val info = _currentModelInfo.value ?: return@withContext Result.failure(IllegalStateException("No model selected."))
        try {
            val prefs = preferencesManager.userPreferencesFlow.first()
            val config = ModelConfig(
                uriString = info.uriString,
                displayName = info.displayName,
                contextLength = minOf(info.contextLength, 4096),
                threadCount = prefs.threadCount,
                maxTokens = prefs.maxTokens,
                temperature = prefs.temperature
            )

            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.LOADING)
            engine.loadModel(config)

            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.LOADED, lastError = null)
            Result.success(Unit)
        } catch (e: Exception) {
            _currentModelInfo.value = info.copy(loadStatus = ModelLoadStatus.FAILED, lastError = e.localizedMessage)
            Result.failure(e)
        }
    }

    suspend fun unloadCurrentModel() = withContext(Dispatchers.IO) {
        engine.unloadModel()
        _currentModelInfo.value = _currentModelInfo.value?.copy(loadStatus = ModelLoadStatus.NOT_LOADED)
    }

    suspend fun removeModelReference() = withContext(Dispatchers.IO) {
        unloadCurrentModel()
        _currentModelInfo.value = null
        preferencesManager.setActiveModelUri(null)
    }
}

