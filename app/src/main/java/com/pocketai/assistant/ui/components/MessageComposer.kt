package com.pocketai.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.domain.model.AttachmentItem
import com.pocketai.assistant.ui.theme.OneUiTheme
import com.pocketai.assistant.ui.theme.PillShape

@Composable
fun MessageComposer(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachClick: () -> Unit,
    isGenerating: Boolean,
    pendingAttachments: List<AttachmentItem>,
    onRemoveAttachment: (AttachmentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OneUiTheme.colors.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 8.dp)
    ) {
        // Attachment previews above input
        AttachmentPreviewRow(
            attachments = pendingAttachments,
            onRemoveAttachment = onRemoveAttachment
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(OneUiTheme.colors.card),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onAttachClick,
                    enabled = !isGenerating,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = if (!isGenerating) OneUiTheme.colors.primary else OneUiTheme.colors.textTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text input container (Pill shaped)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(OneUiTheme.colors.card)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Message Pocket AI…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OneUiTheme.colors.textTertiary
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = OneUiTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(OneUiTheme.colors.primary),
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: Send or Stop
            val canSend = (text.isNotBlank() || pendingAttachments.isNotEmpty()) && !isGenerating

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isGenerating -> OneUiTheme.colors.cardSecondary
                            canSend -> OneUiTheme.colors.primary
                            else -> OneUiTheme.colors.card
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = OneUiTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = canSend,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (canSend) androidx.compose.ui.graphics.Color.White else OneUiTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

