package com.pocketai.assistant.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.domain.model.ModelLoadStatus
import com.pocketai.assistant.ui.components.AppTopBar
import com.pocketai.assistant.ui.components.ChatBubble
import com.pocketai.assistant.ui.components.EmptyChatGreeting
import com.pocketai.assistant.ui.components.MessageComposer
import com.pocketai.assistant.ui.components.ModelStatusCard
import com.pocketai.assistant.ui.theme.PocketTheme

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Document / File Picker launcher for attachments
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addAttachmentUri(uri)
        }
    }

    // Auto-scroll to bottom on new messages or during generation
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PocketTheme.colors.background)
    ) {
        // Pocket AI Top Header
        AppTopBar(
            title = uiState.assistantName,
            subtitle = when (uiState.modelInfo?.loadStatus) {
                ModelLoadStatus.LOADED -> "${uiState.modelInfo?.displayName} • Ready"
                ModelLoadStatus.LOADING, ModelLoadStatus.VALIDATING -> "Loading Model into RAM…"
                ModelLoadStatus.FAILED -> "Model Load Failed • Tap to check"
                ModelLoadStatus.NOT_LOADED -> "${uiState.modelInfo?.displayName} • Tap to load"
                null -> "No Model Selected • Offline"
            },
            actions = {
                IconButton(onClick = onNavigateToModels) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Models",
                        tint = if (uiState.modelInfo?.loadStatus == ModelLoadStatus.LOADED) {
                            PocketTheme.colors.primary
                        } else {
                            PocketTheme.colors.textSecondary
                        }
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = PocketTheme.colors.textPrimary
                    )
                }
            }
        )

        // Banner if model is not loaded
        if (uiState.modelInfo?.loadStatus != ModelLoadStatus.LOADED) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ModelStatusCard(
                    modelInfo = uiState.modelInfo,
                    onLoadClick = { viewModel.loadModel() },
                    onUnloadClick = { viewModel.unloadModel() },
                    onChangeModelClick = onNavigateToModels
                )
            }
        }

        // Messages list or Empty state
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.messages.isEmpty()) {
                EmptyChatGreeting(
                    assistantName = uiState.assistantName,
                    onSuggestedPromptClick = { prompt ->
                        viewModel.onTextChanged(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            onDelete = { viewModel.deleteMessage(it) },
                            onRetry = { viewModel.retryLastAssistantMessage(it) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Message Composer
        MessageComposer(
            text = uiState.inputText,
            onTextChanged = { viewModel.onTextChanged(it) },
            onSend = { viewModel.sendMessage() },
            onStop = { viewModel.stopGeneration() },
            onAttachClick = {
                // Open all supported types
                attachmentPicker.launch(
                    arrayOf(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.ms-powerpoint",
                        "text/plain",
                        "text/csv",
                        "text/markdown",
                        "image/*",
                        "video/*"
                    )
                )
            },
            isGenerating = uiState.isGenerating,
            pendingAttachments = uiState.pendingAttachments,
            onRemoveAttachment = { viewModel.removeAttachment(it) }
        )
    }
}

