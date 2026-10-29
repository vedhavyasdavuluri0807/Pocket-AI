package com.pocketai.assistant.inference.engine

import android.os.Build
import android.util.Log

object LlamaNativeBridge {
    private const val TAG = "LlamaNativeBridge"
    private const val LIB_NAME = "llama"

    var isLibraryLoaded: Boolean = false
        private set

    var loadError: String? = null
        private set

    init {
        try {
            System.loadLibrary(LIB_NAME)
            isLibraryLoaded = true
            Log.i(TAG, "Successfully loaded native library: lib$LIB_NAME.so")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
            loadError = "Native binary 'lib$LIB_NAME.so' not found for device ABIs ($supportedAbis). To enable active token generation on physical hardware, build or place lib$LIB_NAME.so under jniLibs/."
            Log.w(TAG, loadError ?: "")
        } catch (e: Exception) {
            isLibraryLoaded = false
            loadError = "Failed to load native inference engine: ${e.localizedMessage}"
            Log.e(TAG, loadError ?: "", e)
        }
    }

    // JNI signatures for llama.cpp integration (when libllama.so is linked)
    // Note: Kept private / internal to isolate native binding
    @JvmStatic
    external fun nativeInitModel(fd: Int, contextLength: Int, threadCount: Int, useGpu: Boolean): Long

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

    interface NativeTokenCallback {
        fun onToken(token: String): Boolean // return false to stop
    }
}

