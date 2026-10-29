package com.pocketai.assistant

import com.pocketai.assistant.domain.model.AttachmentRef
import com.pocketai.assistant.domain.model.ChatMessage
import com.pocketai.assistant.domain.model.MessageRole
import com.pocketai.assistant.domain.model.MessageStatus
import com.pocketai.assistant.domain.model.ResponseLength
import com.pocketai.assistant.domain.model.ResponseTone
import com.pocketai.assistant.domain.model.UserPreferences
import com.pocketai.assistant.inference.PromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun testBuildSystemPrompt_friendlyToneAndLanguage() {
        val prefs = UserPreferences(
            assistantName = "Galaxy Companion",
            responseTone = ResponseTone.FRIENDLY,
            preferredLanguage = "Spanish",
            casualStyle = true,
            responseLength = ResponseLength.SHORT
        )

        val prompt = PromptBuilder.buildSystemPrompt(prefs)

        assertTrue(prompt.contains("Galaxy Companion"))
        assertTrue(prompt.contains("Warm, welcoming, and friendly"))
        assertTrue(prompt.contains("Respond in Spanish"))
        assertTrue(prompt.contains("Casual, conversational"))
        assertTrue(prompt.contains("1 to 3 short paragraphs"))
        assertTrue(prompt.contains("100% locally and offline"))
    }

    @Test
    fun testBuildSystemPrompt_conciseTone() {
        val prefs = UserPreferences(
            assistantName = "Pocket AI",
            responseTone = ResponseTone.CONCISE,
            preferredLanguage = "English",
            casualStyle = false,
            responseLength = ResponseLength.BALANCED
        )

        val prompt = PromptBuilder.buildSystemPrompt(prefs)

        assertTrue(prompt.contains("Extremely concise, direct"))
        assertFalse(prompt.contains("Respond in English")) // English is default, omitted
    }

    @Test
    fun testBuildAttachmentContext_truncation() {
        val longText = "A".repeat(15_000)
        val attachment = AttachmentRef(
            id = "1",
            uriString = "content://dummy",
            fileName = "sample.txt",
            mimeType = "text/plain",
            sizeBytes = 15_000,
            extractedText = longText
        )

        val message = ChatMessage(
            id = "msg1",
            conversationId = "conv1",
            role = MessageRole.USER,
            content = "Analyze this",
            attachments = listOf(attachment)
        )

        val context = PromptBuilder.buildAttachmentContext(message)

        assertTrue(context.contains("sample.txt"))
        assertTrue(context.contains("truncated to budget"))
        assertTrue(context.contains("characters omitted for context budget"))
    }

    @Test
    fun testBuildChatPrompt_chatMlFormatting() {
        val prefs = UserPreferences(assistantName = "Assistant")
        val history = listOf(
            ChatMessage(
                id = "1",
                conversationId = "conv1",
                role = MessageRole.USER,
                content = "Hello there"
            ),
            ChatMessage(
                id = "2",
                conversationId = "conv1",
                role = MessageRole.ASSISTANT,
                content = "Hi! How can I help?"
            )
        )

        val prompt = PromptBuilder.buildChatPrompt(history, prefs, architecture = "chatml")

        assertTrue(prompt.startsWith("<|im_start|>system"))
        assertTrue(prompt.contains("<|im_start|>user\nHello there<|im_end|>"))
        assertTrue(prompt.contains("<|im_start|>assistant\nHi! How can I help?<|im_end|>"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }
}

