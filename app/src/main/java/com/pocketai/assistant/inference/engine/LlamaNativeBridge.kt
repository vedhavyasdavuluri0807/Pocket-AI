package com.pocketai.assistant.inference.engine

import android.content.Context
import android.os.Build
import android.util.Log

object LlamaNativeBridge {
    private const val TAG = "LlamaNativeBridge"
    private const val LIB_NAME = "pocket_ai_jni"

    var isLibraryLoaded: Boolean = false
        private set

    var loadError: String? = null
        private set

    private var isRuntimeInitialized = false

    init {
        loadNativeLibraries()
    }

    private fun loadNativeLibraries() {
        try {
            // Pre-load dependencies if available as individual shared libraries
            try { System.loadLibrary("ggml-base") } catch (_: Throwable) {}
            try { System.loadLibrary("ggml-cpu") } catch (_: Throwable) {}
            try { System.loadLibrary("ggml") } catch (_: Throwable) {}
            try { System.loadLibrary("llama") } catch (_: Throwable) {}
            try { System.loadLibrary("llama-common") } catch (_: Throwable) {}

            System.loadLibrary(LIB_NAME)
            isLibraryLoaded = true
            Log.i(TAG, "Successfully loaded native library: lib$LIB_NAME.so")
        } catch (e: UnsatisfiedLinkError) {
            // Fallback attempt: if unified into libllama.so
            try {
                System.loadLibrary("llama")
                isLibraryLoaded = true
                Log.i(TAG, "Successfully loaded fallback native library: libllama.so")
            } catch (e2: Throwable) {
                isLibraryLoaded = false
                val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
                loadError = "Native inference binary not found for device ABIs ($supportedAbis). Error: ${e.localizedMessage}"
                Log.w(TAG, loadError ?: "")
            }
        } catch (e: Exception) {
            isLibraryLoaded = false
            loadError = "Failed to load native inference engine: ${e.localizedMessage}"
            Log.e(TAG, loadError ?: "", e)
        }
    }

    fun init(context: Context) {
        if (!isLibraryLoaded) {
            loadNativeLibraries()
        }

        if (isLibraryLoaded && !isRuntimeInitialized) {
            try {
                val libDir = context.applicationInfo.nativeLibraryDir
                Log.i(TAG, "Initializing native runtime with nativeLibDir: $libDir")
                val success = nativeInit(libDir)
                if (success) {
                    isRuntimeInitialized = true
                    Log.i(TAG, "Native runtime initialized successfully")
                } else {
                    val err = nativeGetLastError()
                    Log.w(TAG, "nativeInit returned false. Last error: $err")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "nativeInit warning: ${e.localizedMessage}", e)
            }
        }
    }

    @JvmStatic
    external fun nativeInit(nativeLibDir: String?): Boolean

    @JvmStatic
    external fun nativeLoadModel(
        fd: Int,
        modelPath: String?,
        contextLength: Int,
        threadCount: Int,
        useGpu: Boolean
    ): Long

    @JvmStatic
    external fun nativeInitModel(
        fd: Int,
        contextLength: Int,
        threadCount: Int,
        useGpu: Boolean
    ): Long

    @JvmStatic
    external fun nativeGenerate(
        modelHandle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        callback: NativeTokenCallback
    ): Int

    @JvmStatic
    external fun nativeStop(modelHandle: Long)

    @JvmStatic
    external fun nativeFreeModel(modelHandle: Long)

    @JvmStatic
    external fun nativeGetLastError(): String?

    @JvmStatic
    external fun nativeGetSystemInfo(): String

    interface NativeTokenCallback {
        fun onToken(token: String): Boolean // return false to stop
    }
}
