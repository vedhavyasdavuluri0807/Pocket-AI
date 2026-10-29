package com.pocketai.assistant.inference

import com.pocketai.assistant.domain.model.ChatMessage
import com.pocketai.assistant.domain.model.MessageRole
import com.pocketai.assistant.domain.model.ResponseLength
import com.pocketai.assistant.domain.model.ResponseTone
import com.pocketai.assistant.domain.model.UserPreferences

object PromptBuilder {

    // Conservative default limit for attached document text in characters (~2,500 - 3,000 tokens)
    const val MAX_ATTACHMENT_TEXT_CHARS = 10_000

    fun buildSystemPrompt(preferences: UserPreferences): String {
        val builder = StringBuilder()
        builder.append("You are ${preferences.assistantName}, a helpful and private on-device personal assistant.\n")

        when (preferences.responseTone) {
            ResponseTone.FRIENDLY -> builder.append("Tone: Warm, welcoming, and friendly.\n")
            ResponseTone.CONCISE -> builder.append("Tone: Extremely concise, direct, and to the point. Omit filler words.\n")
            ResponseTone.DETAILED -> builder.append("Tone: Thorough, structured, and explanatory with rich detail.\n")
            ResponseTone.FORMAL -> builder.append("Tone: Professional, polite, and formal.\n")
            ResponseTone.CUSTOM -> {
                if (preferences.customToneDescription.isNotBlank()) {
                    builder.append("Tone: ${preferences.customToneDescription.trim()}\n")
                }
            }
        }

        if (preferences.casualStyle) {
            builder.append("Style: Casual, conversational, and natural.\n")
        }

        when (preferences.responseLength) {
            ResponseLength.SHORT -> builder.append("Length: Keep responses brief, typically 1 to 3 short paragraphs.\n")
            ResponseLength.BALANCED -> builder.append("Length: Balanced length covering essential points clearly.\n")
            ResponseLength.THOROUGH -> builder.append("Length: In-depth and exhaustive coverage.\n")
        }

        if (preferences.preferredLanguage.isNotBlank() && !preferences.preferredLanguage.equals("English", ignoreCase = true)) {
            builder.append("Language: Respond in ${preferences.preferredLanguage.trim()} unless explicitly instructed otherwise.\n")
        }

        if (preferences.userNotes.isNotBlank()) {
            builder.append("User Notes: ${preferences.userNotes.trim()}\n")
        }

        if (preferences.customInstructions.isNotBlank()) {
            builder.append("Additional Instructions:\n${preferences.customInstructions.trim()}\n")
        }

        builder.append("\nPrivacy note: You run 100% locally and offline on the user's phone. Never fabricate facts or pretend to have cloud internet access.")
        return builder.toString().trim()
    }

    fun buildAttachmentContext(message: ChatMessage): String {
        if (message.attachments.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append("\n\n--- User Attachments ---\n")

        for (attachment in message.attachments) {
            builder.append("[Attachment: ${attachment.fileName} (${attachment.mimeType}, ${formatFileSize(attachment.sizeBytes)})]\n")
            if (!attachment.extractedText.isNullOrBlank()) {
                val text = attachment.extractedText
                if (text.length > MAX_ATTACHMENT_TEXT_CHARS) {
                    val truncated = text.substring(0, MAX_ATTACHMENT_TEXT_CHARS)
                    builder.append("Extracted Content (truncated to budget):\n\"\"\"\n")
                    builder.append(truncated)
                    builder.append("\n... [${text.length - MAX_ATTACHMENT_TEXT_CHARS} additional characters omitted for context budget]\n\"\"\"\n")
                } else {
                    builder.append("Extracted Content:\n\"\"\"\n")
                    builder.append(text)
                    builder.append("\n\"\"\"\n")
                }
            } else if (attachment.mimeType.startsWith("image/")) {
                builder.append("Note: Image attachment provided. (Text-only model loaded; image vision requires multimodal projector weights).\n")
            } else if (attachment.mimeType.startsWith("video/")) {
                builder.append("Note: Video attachment provided. (Audio transcription requires offline speech model; frames require vision model).\n")
            }
        }
        builder.append("--- End Attachments ---\n")
        return builder.toString()
    }

    fun buildChatPrompt(
        history: List<ChatMessage>,
        preferences: UserPreferences,
        architecture: String = "llama"
    ): String {
        val systemPrompt = buildSystemPrompt(preferences)

        // Select format based on model architecture
        return when (architecture.lowercase()) {
            "chatml", "qwen", "qwen2" -> buildChatMl(systemPrompt, history)
            "llama", "mistral" -> buildLlamaFormat(systemPrompt, history)
            else -> buildStandardFormat(systemPrompt, history)
        }
    }

    private fun buildChatMl(systemPrompt: String, history: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n")

        // Include recent history (up to last 16 turns to fit context length)
        val recentHistory = history.takeLast(16)
        for (msg in recentHistory) {
            when (msg.role) {
                MessageRole.USER -> {
                    val attachmentContext = buildAttachmentContext(msg)
                    sb.append("<|im_start|>user\n")
                        .append(msg.content)
                        .append(attachmentContext)
                        .append("<|im_end|>\n")
                }
                MessageRole.ASSISTANT -> {
                    if (msg.content.isNotBlank()) {
                        sb.append("<|im_start|>assistant\n")
                            .append(msg.content)
                            .append("<|im_end|>\n")
                    }
                }
                MessageRole.SYSTEM -> {}
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun buildLlamaFormat(systemPrompt: String, history: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("<s>[INST] <<SYS>>\n").append(systemPrompt).append("\n<</SYS>>\n\n")

        val recentHistory = history.takeLast(16)
        var isFirstUser = true
        for (msg in recentHistory) {
            when (msg.role) {
                MessageRole.USER -> {
                    val attachmentContext = buildAttachmentContext(msg)
                    if (isFirstUser) {
                        sb.append(msg.content).append(attachmentContext).append(" [/INST]")
                        isFirstUser = false
                    } else {
                        sb.append("<s>[INST] ").append(msg.content).append(attachmentContext).append(" [/INST]")
                    }
                }
                MessageRole.ASSISTANT -> {
                    if (msg.content.isNotBlank()) {
                        sb.append(" ").append(msg.content).append(" </s>")
                    }
                }
                MessageRole.SYSTEM -> {}
            }
        }
        return sb.toString()
    }

    private fun buildStandardFormat(systemPrompt: String, history: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("System: ").append(systemPrompt).append("\n\n")

        val recentHistory = history.takeLast(16)
        for (msg in recentHistory) {
            when (msg.role) {
                MessageRole.USER -> {
                    val attachmentContext = buildAttachmentContext(msg)
                    sb.append("User: ").append(msg.content).append(attachmentContext).append("\n\n")
                }
                MessageRole.ASSISTANT -> {
                    if (msg.content.isNotBlank()) {
                        sb.append("Assistant: ").append(msg.content).append("\n\n")
                    }
                }
                MessageRole.SYSTEM -> {}
            }
        }
        sb.append("Assistant: ")
        return sb.toString()
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = minOf(digitGroups, units.size - 1)
        return "%.1f %s".format(bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}

