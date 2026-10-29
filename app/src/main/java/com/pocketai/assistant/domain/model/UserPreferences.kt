package com.pocketai.assistant.domain.model

import com.pocketai.assistant.ui.theme.AppThemeMode

enum class ResponseTone {
    FRIENDLY,
    CONCISE,
    DETAILED,
    FORMAL,
    CUSTOM
}

enum class ResponseLength {
    SHORT,
    BALANCED,
    THOROUGH
}

data class UserPreferences(
    val assistantName: String = "Pocket Assistant",
    val responseTone: ResponseTone = ResponseTone.FRIENDLY,
    val customToneDescription: String = "",
    val preferredLanguage: String = "English",
    val customInstructions: String = "",
    val casualStyle: Boolean = false,
    val responseLength: ResponseLength = ResponseLength.BALANCED,
    val userNotes: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val hasCompletedOnboarding: Boolean = false,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val threadCount: Int = 4,
    val activeModelUri: String? = null
)

