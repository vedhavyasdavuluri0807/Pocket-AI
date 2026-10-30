package com.pocketai.assistant

import com.pocketai.assistant.domain.inference.GenerationEvent
import com.pocketai.assistant.domain.inference.GenerationRequest
import com.pocketai.assistant.domain.inference.ModelConfig
import com.pocketai.assistant.domain.inference.ModelRuntimeState
import com.pocketai.assistant.domain.model.ModelFormat
import com.pocketai.assistant.domain.model.ModelInfo
import com.pocketai.assistant.domain.model.ModelLoadStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlamaEngineStateTest {

    @Test
    fun testGenerationRequest_parameters() = runBlocking {
        val request = GenerationRequest(
            prompt = "Hello AI",
            maxTokens = 64,
            temperature = 0.7f
        )
        assertEquals("Hello AI", request.prompt)
        assertEquals(64, request.maxTokens)
        assertEquals(0.7f, request.temperature, 0.001f)
    }

    @Test
    fun testModelConfig_contextAndThreadBudgeting() {
        val config = ModelConfig(
            displayName = "llama-test.gguf",
            uriString = "content://test/llama.gguf",
            contextLength = 4096,
            threadCount = 4,
            useGpu = false,
            temperature = 0.7f
        )

        assertEquals(4096, config.contextLength)
        assertEquals(4, config.threadCount)
        assertFalse(config.useGpu)
        assertEquals(0.7f, config.temperature, 0.001f)
    }

    @Test
    fun testModelRuntimeState_typesAndMessages() {
        val unloaded: ModelRuntimeState = ModelRuntimeState.Unloaded
        assertTrue(unloaded is ModelRuntimeState.Unloaded)

        val loading: ModelRuntimeState = ModelRuntimeState.Loading(progress = 0.5f, message = "Loading weights")
        assertEquals(0.5f, (loading as ModelRuntimeState.Loading).progress, 0.01f)
        assertEquals("Loading weights", loading.message)

        val config = ModelConfig(
            displayName = "model.gguf",
            uriString = "content://1",
            contextLength = 2048,
            threadCount = 2,
            useGpu = false,
            temperature = 0.5f
        )
        val loaded: ModelRuntimeState = ModelRuntimeState.Loaded(config = config, architecture = "llama", ramUsageBytes = 1000000L)
        assertEquals("llama", (loaded as ModelRuntimeState.Loaded).architecture)
        assertEquals(1000000L, loaded.ramUsageBytes)

        val error: ModelRuntimeState = ModelRuntimeState.Error("OOM safety check triggered")
        assertEquals("OOM safety check triggered", (error as ModelRuntimeState.Error).message)
    }

    @Test
    fun testModelLoadStatus_states() {
        assertEquals("NOT_LOADED", ModelLoadStatus.NOT_LOADED.name)
        assertEquals("LOADING", ModelLoadStatus.LOADING.name)
        assertEquals("LOADED", ModelLoadStatus.LOADED.name)
        assertEquals("FAILED", ModelLoadStatus.FAILED.name)
        assertEquals("VALIDATING", ModelLoadStatus.VALIDATING.name)
    }

    @Test
    fun testModelInfo_errorPropagation() {
        val info = ModelInfo(
            uriString = "content://test/model.gguf",
            displayName = "model.gguf",
            format = ModelFormat.GGUF,
            fileSizeBytes = 2_000_000_000L,
            loadStatus = ModelLoadStatus.FAILED,
            lastError = "Native error: Permission denied"
        )

        assertEquals(ModelLoadStatus.FAILED, info.loadStatus)
        assertEquals("Native error: Permission denied", info.lastError)

        val recovered = info.copy(
            loadStatus = ModelLoadStatus.LOADED,
            lastError = null
        )
        assertEquals(ModelLoadStatus.LOADED, recovered.loadStatus)
        assertNull(recovered.lastError)
    }
}
