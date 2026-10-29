package com.pocketai.assistant.inference

import android.content.Context
import android.net.Uri
import com.pocketai.assistant.domain.model.ModelFormat
import com.pocketai.assistant.domain.model.ModelInfo
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GgufValidator {
    // Magic: "GGUF" in ASCII -> 0x46554747 in little-endian integer or 0x47, 0x47, 0x55, 0x46
    private val GGUF_MAGIC = byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte())

    sealed class ValidationResult {
        data class Success(val modelInfo: ModelInfo) : ValidationResult()
        data class Incompatible(val reason: String) : ValidationResult()
        data class Error(val message: String, val throwable: Throwable? = null) : ValidationResult()
    }

    fun inspectModelUri(context: Context, uri: Uri, displayName: String, fileSize: Long): ValidationResult {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                validateStream(inputStream, uri.toString(), displayName, fileSize)
            } ?: ValidationResult.Error("Could not open input stream for model file.")
        } catch (e: SecurityException) {
            ValidationResult.Incompatible("Permission to access the model file was revoked or expired: ${e.localizedMessage}")
        } catch (e: Exception) {
            ValidationResult.Error("Failed to inspect model file: ${e.localizedMessage}", e)
        }
    }

    private fun validateStream(
        inputStream: InputStream,
        uriString: String,
        displayName: String,
        fileSize: Long
    ): ValidationResult {
        val header = ByteArray(24)
        var bytesRead = 0
        while (bytesRead < 24) {
            val count = inputStream.read(header, bytesRead, 24 - bytesRead)
            if (count == -1) break
            bytesRead += count
        }

        if (bytesRead < 24) {
            return ValidationResult.Incompatible("File is too small to be a valid GGUF model (size: $bytesRead bytes).")
        }

        // Check magic bytes
        if (header[0] != GGUF_MAGIC[0] ||
            header[1] != GGUF_MAGIC[1] ||
            header[2] != GGUF_MAGIC[2] ||
            header[3] != GGUF_MAGIC[3]
        ) {
            val magicHex = header.take(4).joinToString("") { "%02X".format(it) }
            return ValidationResult.Incompatible(
                "Invalid model format. Header magic '$magicHex' does not match GGUF ('47475546'). Only GGUF models are supported."
            )
        }

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(4)
        val version = buffer.int
        val tensorCount = buffer.long
        val kvCount = buffer.long

        if (version < 1 || version > 3) {
            return ValidationResult.Incompatible("Unsupported GGUF version ($version). Supported versions: 1, 2, 3.")
        }

        // Read a subset of metadata KV pairs to extract architecture and context length
        var architecture = "unknown"
        var contextLength = 2048
        var modelName = displayName

        try {
            var pairsRead = 0
            val maxPairsToScan = minOf(kvCount, 120L)

            while (pairsRead < maxPairsToScan) {
                val keyLenBytes = readExact(inputStream, 8) ?: break
                val keyLen = ByteBuffer.wrap(keyLenBytes).order(ByteOrder.LITTLE_ENDIAN).long
                if (keyLen <= 0 || keyLen > 1024) break

                val keyBytes = readExact(inputStream, keyLen.toInt()) ?: break
                val key = String(keyBytes, Charsets.UTF_8)

                val valTypeBytes = readExact(inputStream, 4) ?: break
                val valType = ByteBuffer.wrap(valTypeBytes).order(ByteOrder.LITTLE_ENDIAN).int

                when (valType) {
                    // UINT8, INT8
                    0, 1 -> skipExact(inputStream, 1)
                    // UINT16, INT16
                    2, 3 -> skipExact(inputStream, 2)
                    // UINT32, INT32, FLOAT32
                    4, 5, 6 -> {
                        val numBytes = readExact(inputStream, 4) ?: break
                        val num = ByteBuffer.wrap(numBytes).order(ByteOrder.LITTLE_ENDIAN).int
                        if (key.endsWith(".context_length") || key == "context_length") {
                            contextLength = num
                        }
                    }
                    // BOOL
                    7 -> skipExact(inputStream, 1)
                    // STRING
                    8 -> {
                        val strLenBytes = readExact(inputStream, 8) ?: break
                        val strLen = ByteBuffer.wrap(strLenBytes).order(ByteOrder.LITTLE_ENDIAN).long
                        if (strLen in 1..2048) {
                            val strBytes = readExact(inputStream, strLen.toInt()) ?: break
                            val valueStr = String(strBytes, Charsets.UTF_8)
                            if (key == "general.architecture") {
                                architecture = valueStr
                            } else if (key == "general.name") {
                                modelName = valueStr
                            }
                        } else {
                            skipExact(inputStream, strLen)
                        }
                    }
                    // UINT64, INT64, FLOAT64
                    10, 11, 12 -> {
                        val numBytes = readExact(inputStream, 8) ?: break
                        val num = ByteBuffer.wrap(numBytes).order(ByteOrder.LITTLE_ENDIAN).long
                        if (key.endsWith(".context_length") || key == "context_length") {
                            contextLength = num.toInt()
                        }
                    }
                    else -> {
                        // Complex types (ARRAY, etc.) - break scanning to prevent stream misalignment
                        break
                    }
                }
                pairsRead++
            }
        } catch (_: Exception) {
            // Stream scanning partial metadata is acceptable; basic header was already verified
        }

        val info = ModelInfo(
            uriString = uriString,
            displayName = if (modelName.isNotBlank() && modelName != displayName) modelName else displayName,
            format = ModelFormat.GGUF,
            fileSizeBytes = fileSize,
            architecture = architecture,
            contextLength = contextLength,
            tensorCount = tensorCount,
            kvCount = kvCount,
            isAccessible = true
        )

        return ValidationResult.Success(info)
    }

    private fun readExact(stream: InputStream, length: Int): ByteArray? {
        val buffer = ByteArray(length)
        var total = 0
        while (total < length) {
            val r = stream.read(buffer, total, length - total)
            if (r == -1) return null
            total += r
        }
        return buffer
    }

    private fun skipExact(stream: InputStream, length: Long) {
        var remaining = length
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                // Fallback read
                if (stream.read() == -1) break
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }
}

