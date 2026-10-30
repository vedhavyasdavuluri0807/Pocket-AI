package com.pocketai.assistant.inference.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pocketai.assistant.domain.inference.GenerationEvent
import com.pocketai.assistant.domain.inference.GenerationRequest
import com.pocketai.assistant.domain.inference.LocalModelEngine
import com.pocketai.assistant.domain.inference.ModelConfig
import com.pocketai.assistant.domain.inference.ModelRuntimeState
import com.pocketai.assistant.inference.GgufValidator
import com.pocketai.assistant.inference.MemorySafetyGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class LlamaCppEngine(
    private val context: Context
) : LocalModelEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"
    }

    private val _state = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.Unloaded)
    override val state: Flow<ModelRuntimeState> = _state.asStateFlow()

    override val isAvailable: Boolean
        get() = LlamaNativeBridge.isLibraryLoaded

    override val availabilityReason: String
        get() = if (LlamaNativeBridge.isLibraryLoaded) {
            "llama.cpp native runtime is active and ready."
        } else {
            LlamaNativeBridge.loadError ?: "Native inference library is not available."
        }

    private var nativeModelHandle: Long = 0L
    private var activeConfig: ModelConfig? = null
    private val isGenerating = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    override suspend fun loadModel(model: ModelConfig): Unit = withContext(Dispatchers.IO) {
        if (_state.value is ModelRuntimeState.Generating) {
            throw IllegalStateException("Cannot switch or load model while generation is in progress.")
        }

        // Unload existing model if loaded
        if (nativeModelHandle != 0L) {
            unloadModel()
        }

        _state.value = ModelRuntimeState.Loading(progress = 0.1f, message = "Validating model file...")

        val uri = Uri.parse(model.uriString)
        val fileLength = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        } catch (e: Exception) {
            val err = "Cannot access model file: ${e.localizedMessage}"
            _state.value = ModelRuntimeState.Error(err)
            throw IllegalStateException(err, e)
        }

        if (fileLength <= 0) {
            val err = "Selected model file is empty or inaccessible (size: $fileLength bytes)."
            _state.value = ModelRuntimeState.Error(err)
            throw IllegalStateException(err)
        }

        // Validate GGUF format
        val validation = GgufValidator.inspectModelUri(context, uri, model.displayName, fileLength)
        if (validation !is GgufValidator.ValidationResult.Success) {
            val errMsg = when (validation) {
                is GgufValidator.ValidationResult.Incompatible -> validation.reason
                is GgufValidator.ValidationResult.Error -> validation.message
                else -> "Model validation failed."
            }
            _state.value = ModelRuntimeState.Error(errMsg)
            throw IllegalStateException(errMsg)
        }

        val info = validation.modelInfo

        // Check device RAM safety
        val memStatus = MemorySafetyGuard.checkMemory(context, fileLength)
        if (!memStatus.isSafeToLoad) {
            val errMsg = "Insufficient RAM: ${memStatus.recommendation}. App aborted load to prevent crashing the OS."
            _state.value = ModelRuntimeState.Error(errMsg)
            throw IllegalStateException(errMsg)
        }

        _state.value = ModelRuntimeState.Loading(progress = 0.4f, message = "Initializing runtime (${info.architecture})...")

        if (!LlamaNativeBridge.isLibraryLoaded) {
            val errMsg = "Native runtime missing: libpocket_ai_jni.so / libllama.so not available: ${LlamaNativeBridge.loadError}"
            _state.value = ModelRuntimeState.Error(errMsg)
            throw IllegalStateException(errMsg)
        }

        // Ensure runtime is initialized
        LlamaNativeBridge.init(context)

        _state.value = ModelRuntimeState.Loading(progress = 0.6f, message = "Mapping model tensors...")

        var handle = 0L

        // Step 1: Attempt direct zero-copy file descriptor mmap
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val fd = pfd.detachFd()
                handle = LlamaNativeBridge.nativeLoadModel(
                    fd = fd,
                    modelPath = null,
                    contextLength = model.contextLength,
                    threadCount = model.threadCount,
                    useGpu = model.useGpu
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct FD load attempt encountered issue: ${e.localizedMessage}. Proceeding to fallback.", e)
        }

        // Step 2: Fallback to staged cache copy if direct procfs access fails
        if (handle == 0L) {
            _state.value = ModelRuntimeState.Loading(progress = 0.8f, message = "Staging model to app cache fallback...")
            val stagedPath = stageModelToCacheFallback(uri, model.displayName, fileLength)
            if (stagedPath != null) {
                handle = LlamaNativeBridge.nativeLoadModel(
                    fd = -1,
                    modelPath = stagedPath,
                    contextLength = model.contextLength,
                    threadCount = model.threadCount,
                    useGpu = model.useGpu
                )
            }
        }

        if (handle == 0L) {
            val lastErr = LlamaNativeBridge.nativeGetLastError()
            val errMessage = if (!lastErr.isNullOrBlank()) {
                "Failed to initialize model in llama.cpp runtime: $lastErr"
            } else {
                "Failed to initialize model in llama.cpp native runtime."
            }
            _state.value = ModelRuntimeState.Error(errMessage)
            throw IllegalStateException(errMessage)
        }

        nativeModelHandle = handle
        activeConfig = model
        _state.value = ModelRuntimeState.Loaded(
            config = model,
            architecture = info.architecture,
            ramUsageBytes = fileLength
        )
        Log.i(TAG, "Successfully loaded model: ${model.displayName} with handle $handle")
        Unit
    }

    private fun stageModelToCacheFallback(uri: Uri, displayName: String, fileLength: Long): String? {
        return try {
            val modelsDir = File(context.cacheDir, "staged_models").apply { mkdirs() }
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(modelsDir, sanitized)

            if (targetFile.exists() && targetFile.length() == fileLength) {
                Log.i(TAG, "Reusing existing staged model at ${targetFile.absolutePath}")
                return targetFile.absolutePath
            }

            // Clean previous staged models to conserve storage
            modelsDir.listFiles()?.forEach { it.delete() }

            val freeBytes = context.cacheDir.usableSpace
            if (freeBytes < fileLength + 50 * 1024 * 1024) {
                Log.e(TAG, "Insufficient disk space for fallback copy: need $fileLength, available $freeBytes")
                return null
            }

            Log.i(TAG, "Staging model file to ${targetFile.absolutePath}...")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed staging model to cache: ${e.localizedMessage}", e)
            null
        }
    }

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = callbackFlow {
        val handle = nativeModelHandle
        val currentState = _state.value

        if (currentState !is ModelRuntimeState.Loaded || handle == 0L) {
            val errReason = when (currentState) {
                is ModelRuntimeState.Error -> "Model failed to load: ${currentState.message}"
                is ModelRuntimeState.Loading -> "Model is still loading into memory (${currentState.message}). Please wait."
                is ModelRuntimeState.Unloaded -> {
                    val lastErr = LlamaNativeBridge.nativeGetLastError()
                    if (!lastErr.isNullOrBlank()) {
                        "Native runtime error: $lastErr"
                    } else if (!LlamaNativeBridge.isLibraryLoaded) {
                        "Native inference library is not available: ${LlamaNativeBridge.loadError}"
                    } else {
                        "No local model is currently loaded. Please load a valid GGUF model in Model Management."
                    }
                }
                else -> "No local model is currently loaded. Please load a valid GGUF model in Model Management."
            }
            trySend(GenerationEvent.Error(errReason))
            close()
            return@callbackFlow
        }

        if (isGenerating.getAndSet(true)) {
            trySend(GenerationEvent.Error("Generation already in progress. Only one active generation is permitted."))
            close()
            return@callbackFlow
        }

        stopRequested.set(false)
        _state.value = ModelRuntimeState.Generating(request.prompt)

        val fullTextBuilder = StringBuilder()
        var tokenCount = 0
        val startTime = System.currentTimeMillis()

        try {
            val callback = object : LlamaNativeBridge.NativeTokenCallback {
                override fun onToken(token: String): Boolean {
                    if (stopRequested.get()) {
                        return false
                    }
                    tokenCount++
                    fullTextBuilder.append(token)
                    trySend(GenerationEvent.Token(token))

                    // Check stop sequences
                    for (seq in request.stopSequences) {
                        if (fullTextBuilder.endsWith(seq)) {
                            return false
                        }
                    }
                    return true
                }
            }

            val resultCode = LlamaNativeBridge.nativeGenerate(
                modelHandle = handle,
                prompt = request.prompt,
                maxTokens = request.maxTokens,
                temperature = request.temperature,
                callback = callback
            )

            val durationMs = System.currentTimeMillis() - startTime
            if (stopRequested.get()) {
                trySend(GenerationEvent.Interrupted)
            } else if (resultCode != 0) {
                val lastErr = LlamaNativeBridge.nativeGetLastError() ?: "Unknown native error"
                trySend(GenerationEvent.Error("Native generation exited with error code $resultCode: $lastErr"))
            } else {
                trySend(
                    GenerationEvent.Completed(
                        fullText = fullTextBuilder.toString().trim(),
                        tokensGenerated = tokenCount,
                        durationMs = durationMs
                    )
                )
            }
        } catch (e: CancellationException) {
            trySend(GenerationEvent.Interrupted)
        } catch (e: Exception) {
            trySend(GenerationEvent.Error("Generation failed: ${e.localizedMessage}", e))
        } finally {
            isGenerating.set(false)
            activeConfig?.let { cfg ->
                _state.value = ModelRuntimeState.Loaded(cfg, "llama")
            } ?: run {
                _state.value = ModelRuntimeState.Unloaded
            }
            close()
        }

        awaitClose {
            stopRequested.set(true)
            if (handle != 0L) {
                LlamaNativeBridge.nativeStop(handle)
            }
            isGenerating.set(false)
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun stopGeneration() = withContext(Dispatchers.IO) {
        stopRequested.set(true)
        if (nativeModelHandle != 0L) {
            LlamaNativeBridge.nativeStop(nativeModelHandle)
        }
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        stopGeneration()
        val handle = nativeModelHandle
        if (handle != 0L) {
            nativeModelHandle = 0L
            try {
                LlamaNativeBridge.nativeFreeModel(handle)
            } catch (_: Exception) {}
        }
        activeConfig = null
        _state.value = ModelRuntimeState.Unloaded
    }
}
