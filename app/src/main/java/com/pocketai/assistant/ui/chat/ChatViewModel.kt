package com.pocketai.assistant.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketai.assistant.attachments.AttachmentProcessor
import com.pocketai.assistant.data.preferences.PreferencesManager
import com.pocketai.assistant.domain.chat.ChatRepository
import com.pocketai.assistant.domain.inference.GenerationEvent
import com.pocketai.assistant.domain.inference.ModelRuntimeState
import com.pocketai.assistant.domain.model.AttachmentItem
import com.pocketai.assistant.domain.model.ChatMessage
import com.pocketai.assistant.domain.model.ModelInfo
import com.pocketai.assistant.domain.model.ModelLoadStatus
import com.pocketai.assistant.inference.ModelManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val pendingAttachments: List<AttachmentItem> = emptyList(),
    val modelInfo: ModelInfo? = null,
    val engineState: ModelRuntimeState = ModelRuntimeState.Unloaded,
    val assistantName: String = "Pocket Assistant"
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager,
    private val preferencesManager: PreferencesManager,
    private val attachmentProcessor: AttachmentProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        // Observe conversation messages
        chatRepository.getMessagesFlow()
            .onEach { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
            .launchIn(viewModelScope)

        // Observe active model info
        modelManager.currentModelInfo
            .onEach { info ->
                _uiState.value = _uiState.value.copy(modelInfo = info)
            }
            .launchIn(viewModelScope)

        // Observe native inference engine state
        modelManager.engine.state
            .onEach { engineState ->
                _uiState.value = _uiState.value.copy(engineState = engineState)
            }
            .launchIn(viewModelScope)

        // Observe assistant name
        preferencesManager.userPreferencesFlow
            .onEach { prefs ->
                _uiState.value = _uiState.value.copy(assistantName = prefs.assistantName)
            }
            .launchIn(viewModelScope)
    }

    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun addAttachmentUri(uri: Uri) {
        viewModelScope.launch {
            val processed = attachmentProcessor.processUri(uri)
            val currentList = _uiState.value.pendingAttachments.toMutableList()
            currentList.add(processed)
            _uiState.value = _uiState.value.copy(pendingAttachments = currentList)
        }
    }

    fun removeAttachment(item: AttachmentItem) {
        val currentList = _uiState.value.pendingAttachments.toMutableList()
        currentList.removeAll { it.id == item.id }
        _uiState.value = _uiState.value.copy(pendingAttachments = currentList)
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val attachments = state.pendingAttachments

        if ((text.isBlank() && attachments.isEmpty()) || state.isGenerating) return

        // Clear input field and pending attachments
        _uiState.value = _uiState.value.copy(
            inputText = "",
            pendingAttachments = emptyList(),
            isGenerating = true
        )

        val currentModel = modelManager.currentModelInfo.value

        // If a model is chosen but not currently loaded into RAM, auto-load it before sending
        if (currentModel != null && currentModel.loadStatus != ModelLoadStatus.LOADED) {
            viewModelScope.launch {
                val loadResult = modelManager.loadCurrentModel()
                if (loadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                    return@launch
                }
                executeSendMessage(text, attachments)
            }
            return
        }

        executeSendMessage(text, attachments)
    }

    private fun executeSendMessage(text: String, attachments: List<AttachmentItem>) {
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
                chatRepository.sendUserMessage(text, attachments).collect { event ->
                    when (event) {
                        is GenerationEvent.Completed,
                        is GenerationEvent.Error,
                        is GenerationEvent.Interrupted -> {
                            _uiState.value = _uiState.value.copy(isGenerating = false)
                        }
                        is GenerationEvent.Token -> {}
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } finally {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            chatRepository.stopGeneration()
            activeGenerationJob?.cancel()
            _uiState.value = _uiState.value.copy(isGenerating = false)
        }
    }

    fun retryLastAssistantMessage(message: ChatMessage) {
        if (_uiState.value.isGenerating) return

        _uiState.value = _uiState.value.copy(isGenerating = true)
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
                val flow = chatRepository.retryLastAssistantResponse()
                flow?.collect { event ->
                    when (event) {
                        is GenerationEvent.Completed,
                        is GenerationEvent.Error,
                        is GenerationEvent.Interrupted -> {
                            _uiState.value = _uiState.value.copy(isGenerating = false)
                        }
                        is GenerationEvent.Token -> {}
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } finally {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            chatRepository.deleteMessage(message.id)
        }
    }

    fun loadModel() {
        viewModelScope.launch {
            modelManager.loadCurrentModel()
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            modelManager.unloadCurrentModel()
        }
    }
}

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager,
    private val preferencesManager: PreferencesManager,
    private val attachmentProcessor: AttachmentProcessor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(chatRepository, modelManager, preferencesManager, attachmentProcessor) as T
    }
}
