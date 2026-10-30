package com.pocketai.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
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
import com.pocketai.assistant.ui.components.RoundedSettingsCard
import com.pocketai.assistant.ui.components.SettingsCardDivider
import com.pocketai.assistant.ui.components.SettingsRow
import com.pocketai.assistant.ui.theme.AppThemeMode
import com.pocketai.assistant.ui.theme.PocketTheme
import com.pocketai.assistant.ui.theme.StatusError

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToPersonalization: () -> Unit,
    onNavigateToStoragePrivacy: () -> Unit,
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
            title = "Settings",
            subtitle = "Pocket AI preferences",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: AI Model
            SettingsSectionHeader(title = "AI Model")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Active Model",
                    subtitle = uiState.activeModelName,
                    icon = Icons.Default.Memory,
                    onClick = onNavigateToModels
                )
            }

            // Section 2: Personalization
            SettingsSectionHeader(title = "Personalization")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Assistant Identity & Tone",
                    subtitle = "${uiState.preferences.assistantName} • ${uiState.preferences.responseTone.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    icon = Icons.Default.Tune,
                    onClick = onNavigateToPersonalization
                )
            }

            // Section 3: Chat and History
            SettingsSectionHeader(title = "Chat and History")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Stored Messages",
                    subtitle = "${uiState.messageCount} local conversation messages",
                    icon = Icons.Default.ChatBubbleOutline,
                    showChevron = false
                )
                SettingsCardDivider()
                SettingsRow(
                    title = "Clear Chat History",
                    subtitle = "Delete all messages in this conversation",
                    icon = Icons.Default.Delete,
                    onClick = { viewModel.setShowClearHistoryDialog(true) }
                )
            }

            // Section 4 & 5: Attachments, Storage & Privacy
            SettingsSectionHeader(title = "Attachments, Storage & Privacy")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Storage & Privacy Controls",
                    subtitle = "On-device storage usage, file permissions, zero telemetry",
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToStoragePrivacy
                )
            }

            // Section 6: Appearance
            SettingsSectionHeader(title = "Appearance")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Theme Mode",
                    subtitle = when (uiState.preferences.themeMode) {
                        AppThemeMode.SYSTEM -> "Follow system default"
                        AppThemeMode.LIGHT -> "Light theme"
                        AppThemeMode.DARK -> "Dark theme (AMOLED)"
                    },
                    icon = Icons.Default.ColorLens,
                    onClick = {
                        val nextMode = when (uiState.preferences.themeMode) {
                            AppThemeMode.SYSTEM -> AppThemeMode.DARK
                            AppThemeMode.DARK -> AppThemeMode.LIGHT
                            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                        }
                        viewModel.updateThemeMode(nextMode)
                    }
                )
            }

            // Section 7: About
            SettingsSectionHeader(title = "About")
            RoundedSettingsCard {
                SettingsRow(
                    title = "Pocket AI",
                    subtitle = "Version 1.0.0 • Offline Mobile Edition",
                    icon = Icons.Default.Info,
                    showChevron = false
                )
                SettingsCardDivider()
                SettingsRow(
                    title = "Philosophy",
                    subtitle = "100% on-device AI. No server dependency. No data collection.",
                    showChevron = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = PocketTheme.colors.primary,
        modifier = Modifier.padding(start = 10.dp, top = 4.dp)
    )
}

