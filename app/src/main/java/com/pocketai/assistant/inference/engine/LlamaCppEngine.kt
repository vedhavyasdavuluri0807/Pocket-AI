package com.pocketai.assistant.inference.engine

import android.content.Context
import android.net.Uri
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

    override suspend fun loadModel(model: ModelConfig) = withContext(Dispatchers.IO) {
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
            _state.value = ModelRuntimeState.Error("Cannot access model file: ${e.localizedMessage}")
            return@withContext
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
            return@withContext
        }

        val info = validation.modelInfo

        // Check device RAM safety
        val memStatus = MemorySafetyGuard.checkMemory(context, fileLength)
        if (!memStatus.isSafeToLoad) {
            _state.value = ModelRuntimeState.Error(
                "Insufficient RAM: ${memStatus.recommendation}. App aborted load to prevent crashing the OS."
            )
            return@withContext
        }

        _state.value = ModelRuntimeState.Loading(progress = 0.5f, message = "Initializing runtime (${info.architecture})...")

        if (!LlamaNativeBridge.isLibraryLoaded) {
            // Honest reporting per prompt requirements: do not fake generation, report exact requirement
            _state.value = ModelRuntimeState.Error(
                "Native runtime missing: The GGUF model file was successfully validated (${info.architecture}, ${info.tensorCount} tensors, context ${info.contextLength}), but libllama.so is not bundled in this build. Please package libllama.so into the APK to execute inferences on hardware."
            )
            return@withContext
        }

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Could not open file descriptor for model")

            val fd = pfd.detachFd()
            val handle = LlamaNativeBridge.nativeInitModel(
                fd = fd,
                contextLength = model.contextLength,
                threadCount = model.threadCount,
                useGpu = model.useGpu
            )

            if (handle == 0L) {
                _state.value = ModelRuntimeState.Error("Failed to initialize model in llama.cpp native runtime.")
                return@withContext
            }

            nativeModelHandle = handle
            activeConfig = model
            _state.value = ModelRuntimeState.Loaded(
                config = model,
                architecture = info.architecture,
                ramUsageBytes = fileLength
            )
        } catch (e: Exception) {
            _state.value = ModelRuntimeState.Error("Error during model load: ${e.localizedMessage}")
        }
    }

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = callbackFlow {
        val handle = nativeModelHandle
        val currentState = _state.value

        if (currentState !is ModelRuntimeState.Loaded || handle == 0L) {
            trySend(GenerationEvent.Error("No local model is currently loaded. Please load a valid GGUF model in Model Management."))
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
                trySend(GenerationEvent.Error("Native generation exited with error code $resultCode."))
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

