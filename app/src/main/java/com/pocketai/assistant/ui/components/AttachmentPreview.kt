package com.pocketai.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.domain.model.AttachmentItem
import com.pocketai.assistant.domain.model.AttachmentType
import com.pocketai.assistant.ui.theme.OneUiTheme
import com.pocketai.assistant.ui.theme.PillShape

@Composable
fun AttachmentPreviewRow(
    attachments: List<AttachmentItem>,
    onRemoveAttachment: (AttachmentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(attachments, key = { it.id }) { item ->
            AttachmentChip(
                item = item,
                onRemove = { onRemoveAttachment(item) }
            )
        }
    }
}

@Composable
fun AttachmentChip(
    item: AttachmentItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (item.type) {
        AttachmentType.IMAGE -> Icons.Default.Image
        AttachmentType.VIDEO -> Icons.Default.Videocam
        else -> Icons.Default.Description
    }

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(OneUiTheme.colors.cardSecondary)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OneUiTheme.colors.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.width(130.dp)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = OneUiTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatSize(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = OneUiTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(OneUiTheme.colors.divider),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = OneUiTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = minOf(digitGroups, units.size - 1)
    return "%.1f %s".format(bytes / Math.pow(1024.0, index.toDouble()), units[index])
}

