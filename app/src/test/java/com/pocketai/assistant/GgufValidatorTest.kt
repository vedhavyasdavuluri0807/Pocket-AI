package com.pocketai.assistant

import com.pocketai.assistant.domain.model.ModelFormat
import com.pocketai.assistant.inference.GgufValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufValidatorTest {

    @Test
    fun testGgufHeaderValidation_invalidMagic() {
        // Construct stream with invalid magic "TEST"
        val bytes = ByteArray(32)
        bytes[0] = 'T'.code.toByte()
        bytes[1] = 'E'.code.toByte()
        bytes[2] = 'S'.code.toByte()
        bytes[3] = 'T'.code.toByte()

        val stream = ByteArrayInputStream(bytes)

        // Reflection or mock invocation of stream validation
        val method = GgufValidator::class.java.getDeclaredMethod(
            "validateStream",
            java.io.InputStream::class.java,
            String::class.java,
            String::class.java,
            Long::class.javaPrimitiveType
        )
        method.isAccessible = true

        val result = method.invoke(GgufValidator, stream, "content://test", "test.bin", 32L)
        assertTrue(result is GgufValidator.ValidationResult.Incompatible)
        val incompatible = result as GgufValidator.ValidationResult.Incompatible
        assertTrue(incompatible.reason.contains("does not match GGUF"))
    }

    @Test
    fun testGgufHeaderValidation_validMagicAndVersion() {
        // Construct valid GGUF header: 'G','G','U','F', version = 3, tensorCount = 10, kvCount = 0
        val buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put('G'.code.toByte())
        buffer.put('G'.code.toByte())
        buffer.put('U'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(3) // version
        buffer.putLong(10L) // tensor count
        buffer.putLong(0L) // kv count

        val stream = ByteArrayInputStream(buffer.array())

        val method = GgufValidator::class.java.getDeclaredMethod(
            "validateStream",
            java.io.InputStream::class.java,
            String::class.java,
            String::class.java,
            Long::class.javaPrimitiveType
        )
        method.isAccessible = true

        val result = method.invoke(GgufValidator, stream, "content://test", "tiny-llama.gguf", 1024L)
        assertTrue(result is GgufValidator.ValidationResult.Success)
        val success = result as GgufValidator.ValidationResult.Success
        assertEquals(ModelFormat.GGUF, success.modelInfo.format)
        assertEquals(10L, success.modelInfo.tensorCount)
        assertEquals(0L, success.modelInfo.kvCount)
        assertEquals("tiny-llama.gguf", success.modelInfo.displayName)
    }

    @Test
    fun testGgufHeaderValidation_fileTooSmall() {
        val bytes = ByteArray(10)
        val stream = ByteArrayInputStream(bytes)

        val method = GgufValidator::class.java.getDeclaredMethod(
            "validateStream",
            java.io.InputStream::class.java,
            String::class.java,
            String::class.java,
            Long::class.javaPrimitiveType
        )
        method.isAccessible = true

        val result = method.invoke(GgufValidator, stream, "content://test", "empty.gguf", 10L)
        assertTrue(result is GgufValidator.ValidationResult.Incompatible)
        val incompatible = result as GgufValidator.ValidationResult.Incompatible
        assertTrue(incompatible.reason.contains("too small"))
    }
}

