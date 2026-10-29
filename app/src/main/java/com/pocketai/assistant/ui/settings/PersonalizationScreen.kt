package com.pocketai.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.domain.model.ResponseLength
import com.pocketai.assistant.domain.model.ResponseTone
import com.pocketai.assistant.ui.components.AppTopBar
import com.pocketai.assistant.ui.components.RoundedSettingsCard
import com.pocketai.assistant.ui.components.SettingsCardDivider
import com.pocketai.assistant.ui.components.SettingsRow
import com.pocketai.assistant.ui.theme.OneUiTheme
import com.pocketai.assistant.ui.theme.PillShape

@Composable
fun PersonalizationScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    val scrollState = rememberScrollState()

    var assistantNameInput by remember(prefs.assistantName) { mutableStateOf(prefs.assistantName) }
    var languageInput by remember(prefs.preferredLanguage) { mutableStateOf(prefs.preferredLanguage) }
    var customToneInput by remember(prefs.customToneDescription) { mutableStateOf(prefs.customToneDescription) }
    var customInstructionsInput by remember(prefs.customInstructions) { mutableStateOf(prefs.customInstructions) }
    var userNotesInput by remember(prefs.userNotes) { mutableStateOf(prefs.userNotes) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUiTheme.colors.background)
    ) {
        AppTopBar(
            title = "Personalization",
            subtitle = "Customize assistant personality & prompts",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Header
            SettingsSectionHeader(title = "Assistant Identity")
            RoundedSettingsCard {
                OneUiInputField(
                    label = "Display Name",
                    value = assistantNameInput,
                    onValueChange = {
                        assistantNameInput = it
                        viewModel.updateAssistantName(it)
                    },
                    placeholder = "e.g. Pocket Assistant"
                )
                SettingsCardDivider()
                OneUiInputField(
                    label = "Preferred Response Language",
                    value = languageInput,
                    onValueChange = {
                        languageInput = it
                        viewModel.updatePreferredLanguage(it)
                    },
                    placeholder = "e.g. English, Spanish, German, French"
                )
            }

            // Response Tone
            SettingsSectionHeader(title = "Response Tone")
            RoundedSettingsCard {
                ResponseTone.values().forEachIndexed { index, tone ->
                    ToneOptionRow(
                        tone = tone,
                        isSelected = prefs.responseTone == tone,
                        onSelect = { viewModel.updateResponseTone(tone, customToneInput) }
                    )
                    if (index < ResponseTone.values().size - 1) {
                        SettingsCardDivider()
                    }
                }

                if (prefs.responseTone == ResponseTone.CUSTOM) {
                    SettingsCardDivider()
                    OneUiInputField(
                        label = "Custom Tone Description",
                        value = customToneInput,
                        onValueChange = {
                            customToneInput = it
                            viewModel.updateResponseTone(ResponseTone.CUSTOM, it)
                        },
                        placeholder = "e.g. Analytical, enthusiastic, poetic"
                    )
                }
            }

            // Conversational Style & Length
            SettingsSectionHeader(title = "Conversational Style & Length")
            RoundedSettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Casual Conversational Style",
                            style = MaterialTheme.typography.titleMedium,
                            color = OneUiTheme.colors.textPrimary
                        )
                        Text(
                            text = "Use informal, conversational expressions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OneUiTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = prefs.casualStyle,
                        onCheckedChange = { viewModel.updateCasualStyle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = OneUiTheme.colors.primary
                        )
                    )
                }

                SettingsCardDivider()

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Response Length Preference",
                        style = MaterialTheme.typography.titleMedium,
                        color = OneUiTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResponseLength.values().forEach { len ->
                            val isSelected = prefs.responseLength == len
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(
                                        if (isSelected) OneUiTheme.colors.primary else OneUiTheme.colors.cardSecondary
                                    )
                                    .clickable { viewModel.updateResponseLength(len) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = len.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else OneUiTheme.colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // User Preference Notes & System Instructions
            SettingsSectionHeader(title = "Custom System Instructions & User Context")
            RoundedSettingsCard {
                OneUiInputField(
                    label = "Custom System Instructions",
                    value = customInstructionsInput,
                    onValueChange = {
                        customInstructionsInput = it
                        viewModel.updateCustomInstructions(it)
                    },
                    placeholder = "Instructions injected into system prompt on every generation...",
                    maxLines = 4
                )
                SettingsCardDivider()
                OneUiInputField(
                    label = "User Notes / Context",
                    value = userNotesInput,
                    onValueChange = {
                        userNotesInput = it
                        viewModel.updateUserNotes(it)
                    },
                    placeholder = "Preferences, interests, or background information the assistant should know...",
                    maxLines = 4
                )
            }

            // Clarification Note
            RoundedSettingsCard {
                SettingsRow(
                    title = "How Memory Works",
                    subtitle = "Preferences and notes are injected into the model's system prompt during generation. They do not alter the underlying model weights and are kept strictly on your device.",
                    icon = Icons.Default.Info,
                    showChevron = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ToneOptionRow(
    tone: ResponseTone,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val title = when (tone) {
        ResponseTone.FRIENDLY -> "Friendly (Warm & Encouraging)"
        ResponseTone.CONCISE -> "Concise (Direct & Brief)"
        ResponseTone.DETAILED -> "Detailed (Comprehensive Explanations)"
        ResponseTone.FORMAL -> "Formal (Polite & Professional)"
        ResponseTone.CUSTOM -> "Custom Tone"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = OneUiTheme.colors.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = OneUiTheme.colors.textPrimary
        )
    }
}

@Composable
fun OneUiInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int = 1
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OneUiTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(OneUiTheme.colors.cardSecondary)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUiTheme.colors.textTertiary
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OneUiTheme.colors.textPrimary),
                cursorBrush = SolidColor(OneUiTheme.colors.primary),
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

