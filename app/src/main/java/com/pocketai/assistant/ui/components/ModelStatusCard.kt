package com.pocketai.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.domain.model.ModelInfo
import com.pocketai.assistant.domain.model.ModelLoadStatus
import com.pocketai.assistant.ui.theme.PocketCardShape
import com.pocketai.assistant.ui.theme.PocketTheme
import com.pocketai.assistant.ui.theme.PillShape
import com.pocketai.assistant.ui.theme.StatusError
import com.pocketai.assistant.ui.theme.StatusSuccess
import com.pocketai.assistant.ui.theme.StatusWarning

@Composable
fun ModelStatusCard(
    modelInfo: ModelInfo?,
    onLoadClick: () -> Unit,
    onUnloadClick: () -> Unit,
    onChangeModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PocketCardShape)
            .background(PocketTheme.colors.card)
            .padding(18.dp)
    ) {
        if (modelInfo == null) {
            // Empty / No Model State
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StatusWarning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No AI Model Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PocketTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select a GGUF model from device storage to enable offline chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PocketTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PocketActionButton(
                text = "Select Model File",
                onClick = onChangeModelClick,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Model Configured State
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = PocketTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = modelInfo.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${modelInfo.architecture.uppercase()} • ${formatSize(modelInfo.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PocketTheme.colors.textSecondary
                        )
                    }
                }

                // Status Pill
                StatusBadge(status = modelInfo.loadStatus)
            }

            if (!modelInfo.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(StatusError.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modelInfo.lastError,
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusError,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (modelInfo.loadStatus) {
                    ModelLoadStatus.LOADED -> {
                        PocketSecondaryButton(
                            text = "Unload",
                            onClick = onUnloadClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModelLoadStatus.LOADING, ModelLoadStatus.VALIDATING -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(PillShape)
                                .background(PocketTheme.colors.cardSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PocketTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    else -> {
                        PocketActionButton(
                            text = "Load Model",
                            onClick = onLoadClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PocketSecondaryButton(
                    text = "Change",
                    onClick = onChangeModelClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: ModelLoadStatus) {
    val (color, text, icon) = when (status) {
        ModelLoadStatus.LOADED -> Triple(StatusSuccess, "Loaded", Icons.Default.CheckCircle)
        ModelLoadStatus.LOADING, ModelLoadStatus.VALIDATING -> Triple(PocketTheme.colors.primary, "Loading…", null)
        ModelLoadStatus.FAILED -> Triple(StatusError, "Failed", Icons.Default.ErrorOutline)
        ModelLoadStatus.NOT_LOADED -> Triple(StatusWarning, "Unloaded", Icons.Default.WarningAmber)
    }

    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun PocketActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(PillShape)
            .background(PocketTheme.colors.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PocketSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(PillShape)
            .background(PocketTheme.colors.cardSecondary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = PocketTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = minOf(digitGroups, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, index.toDouble()), units[index])
}

