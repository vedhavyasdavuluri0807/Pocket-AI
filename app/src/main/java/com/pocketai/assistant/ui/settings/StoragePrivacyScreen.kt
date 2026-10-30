package com.pocketai.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.ui.components.AppTopBar
import com.pocketai.assistant.ui.components.PocketSecondaryButton
import com.pocketai.assistant.ui.components.RoundedSettingsCard
import com.pocketai.assistant.ui.components.SettingsCardDivider
import com.pocketai.assistant.ui.components.SettingsRow
import com.pocketai.assistant.ui.theme.PocketTheme
import com.pocketai.assistant.ui.theme.StatusError

@Composable
fun StoragePrivacyScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PocketTheme.colors.background)
    ) {
        AppTopBar(
            title = "Storage & Privacy",
            subtitle = "Zero telemetry & local data management",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Storage Info
            SettingsSectionHeader(title = "Local Storage Architecture")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Conversation Database",
                    subtitle = "Encrypted-at-rest SQLite (Room) with ${uiState.messageCount} saved messages",
                    icon = Icons.Default.Storage,
                    showChevron = false
                )
                SettingsCardDivider()
                SettingsRow(
                    title = "File Access Policy",
                    subtitle = "Scoped Storage Access Framework (SAF) only. Never requests broad all-files permissions.",
                    icon = Icons.Default.Folder,
                    showChevron = false
                )
            }

            // Privacy Guarantees
            SettingsSectionHeader(title = "Privacy Commitments")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Zero Remote Network Calls",
                    subtitle = "No internet permission in manifest. No telemetry, analytics, crash log upload, or ad trackers.",
                    icon = Icons.Default.Security,
                    showChevron = false
                )
                SettingsCardDivider()
                SettingsRow(
                    title = "100% On-Device Processing",
                    subtitle = "All model generation, token streaming, and document text parsing run directly on your phone hardware.",
                    icon = Icons.Default.Lock,
                    showChevron = false
                )
            }

            // Data Deletion Controls
            SettingsSectionHeader(title = "Data Management")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Clear Chat History",
                    subtitle = "Permanently delete all stored messages and attachment references",
                    icon = Icons.Default.DeleteForever,
                    onClick = { viewModel.setShowClearHistoryDialog(true) }
                )
                SettingsCardDivider()
                SettingsRow(
                    title = "Reset All App Data",
                    subtitle = "Clear conversations, model references, and personal preferences",
                    icon = Icons.Default.DeleteForever,
                    onClick = { viewModel.setShowClearAllDataDialog(true) }
                )
            }

            // Model Storage Explanation
            RoundedSettingsCard {
                SettingsRow(
                    title = "Important Notice on Models",
                    subtitle = "When you 'Remove Reference' to a model, Pocket AI removes its URI reference and clears its cached state. The actual model file on your phone is NEVER deleted by this app.",
                    showChevron = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Dialog: Reset All Data
    if (uiState.showClearAllDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearAllDataDialog(false) },
            title = { Text("Reset All App Data?") },
            text = { Text("This will delete all conversations, unlink the active model reference, and reset your assistant settings. The model file on your device will remain intact.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData() }) {
                    Text("Reset All", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearAllDataDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Clear History
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearHistoryDialog(false) },
            title = { Text("Clear Chat History?") },
            text = { Text("This will permanently remove all messages in your current conversation. Your loaded model and settings will be preserved.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearChatHistory() }) {
                    Text("Clear", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearHistoryDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

