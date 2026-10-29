package com.pocketai.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketai.assistant.data.preferences.PreferencesManager
import com.pocketai.assistant.domain.chat.ChatRepository
import com.pocketai.assistant.domain.model.ResponseLength
import com.pocketai.assistant.domain.model.ResponseTone
import com.pocketai.assistant.domain.model.UserPreferences
import com.pocketai.assistant.inference.ModelManager
import com.pocketai.assistant.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val messageCount: Int = 0,
    val activeModelName: String = "None Selected",
    val showClearHistoryDialog: Boolean = false,
    val showClearAllDataDialog: Boolean = false
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        preferencesManager.userPreferencesFlow
            .onEach { prefs ->
                _uiState.value = _uiState.value.copy(preferences = prefs)
            }
            .launchIn(viewModelScope)

        modelManager.currentModelInfo
            .onEach { info ->
                _uiState.value = _uiState.value.copy(
                    activeModelName = info?.displayName ?: "None Selected"
                )
            }
            .launchIn(viewModelScope)

        loadMessageCount()
    }

    fun loadMessageCount() {
        viewModelScope.launch {
            val count = chatRepository.getMessageCount()
            _uiState.value = _uiState.value.copy(messageCount = count)
        }
    }

    fun updateAssistantName(name: String) {
        viewModelScope.launch {
            preferencesManager.updateAssistantName(name)
        }
    }

    fun updateResponseTone(tone: ResponseTone, customDesc: String = "") {
        viewModelScope.launch {
            preferencesManager.updateResponseTone(tone, customDesc)
        }
    }

    fun updatePreferredLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.updatePreferredLanguage(language)
        }
    }

    fun updateCustomInstructions(instructions: String) {
        viewModelScope.launch {
            preferencesManager.updateCustomInstructions(instructions)
        }
    }

    fun updateCasualStyle(casual: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateCasualStyle(casual)
        }
    }

    fun updateResponseLength(length: ResponseLength) {
        viewModelScope.launch {
            preferencesManager.updateResponseLength(length)
        }
    }

    fun updateUserNotes(notes: String) {
        viewModelScope.launch {
            preferencesManager.updateUserNotes(notes)
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(mode)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            loadMessageCount()
            _uiState.value = _uiState.value.copy(showClearHistoryDialog = false)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            modelManager.removeModelReference()
            preferencesManager.clearAll()
            loadMessageCount()
            _uiState.value = _uiState.value.copy(showClearAllDataDialog = false)
        }
    }

    fun setShowClearHistoryDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = show)
    }

    fun setShowClearAllDataDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearAllDataDialog = show)
    }
}

class SettingsViewModelFactory(
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(preferencesManager, chatRepository, modelManager) as T
    }
}

