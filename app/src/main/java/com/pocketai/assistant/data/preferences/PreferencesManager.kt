package com.pocketai.assistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pocketai.assistant.domain.model.ResponseLength
import com.pocketai.assistant.domain.model.ResponseTone
import com.pocketai.assistant.domain.model.UserPreferences
import com.pocketai.assistant.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pocket_ai_prefs")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val RESPONSE_TONE = stringPreferencesKey("response_tone")
        val CUSTOM_TONE_DESC = stringPreferencesKey("custom_tone_desc")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val CUSTOM_INSTRUCTIONS = stringPreferencesKey("custom_instructions")
        val CASUAL_STYLE = booleanPreferencesKey("casual_style")
        val RESPONSE_LENGTH = stringPreferencesKey("response_length")
        val USER_NOTES = stringPreferencesKey("user_notes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val ACTIVE_MODEL_URI = stringPreferencesKey("active_model_uri")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val THREAD_COUNT = intPreferencesKey("thread_count")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val toneString = prefs[PreferencesKeys.RESPONSE_TONE] ?: ResponseTone.FRIENDLY.name
            val tone = try {
                ResponseTone.valueOf(toneString)
            } catch (_: Exception) {
                ResponseTone.FRIENDLY
            }

            val lengthString = prefs[PreferencesKeys.RESPONSE_LENGTH] ?: ResponseLength.BALANCED.name
            val length = try {
                ResponseLength.valueOf(lengthString)
            } catch (_: Exception) {
                ResponseLength.BALANCED
            }

            val themeString = prefs[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val themeMode = try {
                AppThemeMode.valueOf(themeString)
            } catch (_: Exception) {
                AppThemeMode.SYSTEM
            }

            UserPreferences(
                assistantName = prefs[PreferencesKeys.ASSISTANT_NAME] ?: "Pocket Assistant",
                responseTone = tone,
                customToneDescription = prefs[PreferencesKeys.CUSTOM_TONE_DESC] ?: "",
                preferredLanguage = prefs[PreferencesKeys.PREFERRED_LANGUAGE] ?: "English",
                customInstructions = prefs[PreferencesKeys.CUSTOM_INSTRUCTIONS] ?: "",
                casualStyle = prefs[PreferencesKeys.CASUAL_STYLE] ?: false,
                responseLength = length,
                userNotes = prefs[PreferencesKeys.USER_NOTES] ?: "",
                themeMode = themeMode,
                hasCompletedOnboarding = prefs[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
                maxTokens = prefs[PreferencesKeys.MAX_TOKENS] ?: 512,
                temperature = prefs[PreferencesKeys.TEMPERATURE] ?: 0.7f,
                threadCount = prefs[PreferencesKeys.THREAD_COUNT] ?: 4,
                activeModelUri = prefs[PreferencesKeys.ACTIVE_MODEL_URI]
            )
        }

    suspend fun updateAssistantName(name: String) {
        context.dataStore.edit { it[PreferencesKeys.ASSISTANT_NAME] = name.trim() }
    }

    suspend fun updateResponseTone(tone: ResponseTone, customDesc: String = "") {
        context.dataStore.edit {
            it[PreferencesKeys.RESPONSE_TONE] = tone.name
            it[PreferencesKeys.CUSTOM_TONE_DESC] = customDesc.trim()
        }
    }

    suspend fun updatePreferredLanguage(language: String) {
        context.dataStore.edit { it[PreferencesKeys.PREFERRED_LANGUAGE] = language.trim() }
    }

    suspend fun updateCustomInstructions(instructions: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_INSTRUCTIONS] = instructions.trim() }
    }

    suspend fun updateCasualStyle(casual: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CASUAL_STYLE] = casual }
    }

    suspend fun updateResponseLength(length: ResponseLength) {
        context.dataStore.edit { it[PreferencesKeys.RESPONSE_LENGTH] = length.name }
    }

    suspend fun updateUserNotes(notes: String) {
        context.dataStore.edit { it[PreferencesKeys.USER_NOTES] = notes.trim() }
    }

    suspend fun updateThemeMode(theme: AppThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = theme.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { it[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun setActiveModelUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) {
                it[PreferencesKeys.ACTIVE_MODEL_URI] = uri
            } else {
                it.remove(PreferencesKeys.ACTIVE_MODEL_URI)
            }
        }
    }

    suspend fun updateInferenceSettings(maxTokens: Int, temperature: Float, threadCount: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.MAX_TOKENS] = maxTokens
            it[PreferencesKeys.TEMPERATURE] = temperature
            it[PreferencesKeys.THREAD_COUNT] = threadCount
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

