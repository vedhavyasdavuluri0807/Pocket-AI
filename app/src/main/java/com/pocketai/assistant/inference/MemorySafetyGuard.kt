package com.pocketai.assistant.inference

import android.app.ActivityManager
import android.content.Context

object MemorySafetyGuard {

    data class MemoryStatus(
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val isLowMemory: Boolean,
        val thresholdBytes: Long,
        val isSafeToLoad: Boolean,
        val recommendation: String
    )

    fun checkMemory(context: Context, modelSizeBytes: Long): MemoryStatus {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availRam = memoryInfo.availMem
        val isLowMem = memoryInfo.lowMemory

        // Typically, GGUF models need at least 1.15x their file size in RAM for context buffers and tensors
        val requiredRamEstimate = (modelSizeBytes * 1.15).toLong()

        val isSafe = when {
            isLowMem -> false
            availRam < requiredRamEstimate -> false
            else -> true
        }

        val totalGb = "%.1f".format(totalRam / (1024.0 * 1024.0 * 1024.0))
        val availGb = "%.1f".format(availRam / (1024.0 * 1024.0 * 1024.0))
        val modelGb = "%.1f".format(modelSizeBytes / (1024.0 * 1024.0 * 1024.0))

        val recommendation = when {
            isLowMem -> {
                "Device is currently in critical low-memory state ($availGb GB free of $totalGb GB). Close background apps before attempting to load this model."
            }
            availRam < requiredRamEstimate -> {
                "Model size ($modelGb GB) exceeds currently available RAM ($availGb GB free of $totalGb GB total). Loading this model may cause the Android system to terminate the app."
            }
            modelSizeBytes > (totalRam * 0.7) -> {
                "Caution: Model occupies over 70% of total system RAM ($totalGb GB). Use a lower context size (e.g. 2048) and fewer threads (2-4) to prevent memory pressure."
            }
            else -> {
                "Memory headroom is sufficient ($availGb GB free of $totalGb GB)."
            }
        }

        return MemoryStatus(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            isLowMemory = isLowMem,
            thresholdBytes = memoryInfo.threshold,
            isSafeToLoad = isSafe,
            recommendation = recommendation
        )
    }
}

