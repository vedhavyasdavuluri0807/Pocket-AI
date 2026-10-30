package com.pocketai.assistant.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.assistant.domain.model.AttachmentRef
import com.pocketai.assistant.domain.model.ChatMessage
import com.pocketai.assistant.domain.model.MessageRole
import com.pocketai.assistant.domain.model.MessageStatus
import com.pocketai.assistant.ui.theme.BubbleAssistantShape
import com.pocketai.assistant.ui.theme.BubbleUserShape
import com.pocketai.assistant.ui.theme.PocketTheme
import com.pocketai.assistant.ui.theme.PillShape
import com.pocketai.assistant.ui.theme.StatusError
import com.pocketai.assistant.ui.theme.UserBubbleColor
import com.pocketai.assistant.ui.theme.UserBubbleText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    onDelete: (ChatMessage) -> Unit,
    onRetry: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) BubbleUserShape else BubbleAssistantShape
    val bubbleBackground = if (isUser) UserBubbleColor else PocketTheme.colors.card
    val textColor = if (isUser) UserBubbleText else PocketTheme.colors.textPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .clip(bubbleShape)
                    .background(bubbleBackground)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showContextMenu = true }
                    )
                    .padding(14.dp)
            ) {
                // If message has attachments
                if (message.attachments.isNotEmpty()) {
                    MessageAttachmentBadges(attachments = message.attachments, isUser = isUser)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (message.content.isNotBlank()) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    } else {
                        MarkdownMessageBody(
                            content = message.content,
                            textColor = textColor
                        )
                    }
                }

                // Streaming indicator
                if (message.status == MessageStatus.STREAMING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LoadingIndicator()
                }

                // Error State
                if (message.status == MessageStatus.ERROR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = message.errorMessage ?: "Generation failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusError
                        )
                    }

                    if (onRetry != null && !isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(PocketTheme.colors.cardSecondary)
                                .combinedClickable(onClick = { onRetry(message) })
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = PocketTheme.colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall,
                                color = PocketTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Timestamp
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) textColor.copy(alpha = 0.7f) else PocketTheme.colors.textTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Long-press Context Menu
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy message") },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Pocket AI Message", message.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    onClick = {
                        onDelete(message)
                        showContextMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun MessageAttachmentBadges(
    attachments: List<AttachmentRef>,
    isUser: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (att in attachments) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isUser) UserBubbleColor.copy(alpha = 0.85f) else PocketTheme.colors.cardSecondary
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isUser) UserBubbleText else PocketTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = att.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) UserBubbleText else PocketTheme.colors.textPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MarkdownMessageBody(
    content: String,
    textColor: androidx.compose.ui.graphics.Color
) {
    val context = LocalContext.current
    // Render markdown blocks: Detect code blocks ```...``` vs regular text
    val parts = splitCodeBlocks(content)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (part in parts) {
            if (part.isCodeBlock) {
                CodeBlockView(
                    code = part.content,
                    language = part.language ?: "code",
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", part.content))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Text(
                    text = part.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun CodeBlockView(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PocketTheme.colors.codeBlockBackground)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = PocketTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp)
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = PocketTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = PocketTheme.colors.textPrimary
            )
        }
    }
}

private data class MarkdownBlock(
    val content: String,
    val isCodeBlock: Boolean,
    val language: String? = null
)

private fun splitCodeBlocks(text: String): List<MarkdownBlock> {
    val list = mutableListOf<MarkdownBlock>()
    val delimiter = "```"
    var cursor = 0

    while (cursor < text.length) {
        val startIdx = text.indexOf(delimiter, cursor)
        if (startIdx == -1) {
            list.add(MarkdownBlock(content = text.substring(cursor), isCodeBlock = false))
            break
        }

        if (startIdx > cursor) {
            list.add(MarkdownBlock(content = text.substring(cursor, startIdx), isCodeBlock = false))
        }

        val langEnd = text.indexOf('\n', startIdx + delimiter.length)
        val endIdx = text.indexOf(delimiter, if (langEnd != -1) langEnd else startIdx + delimiter.length)

        if (endIdx == -1) {
            // Unclosed code block (e.g. during active streaming)
            val code = text.substring(startIdx + delimiter.length)
            list.add(MarkdownBlock(content = code, isCodeBlock = true, language = "code"))
            break
        }

        val lang = if (langEnd in (startIdx + delimiter.length) until endIdx) {
            text.substring(startIdx + delimiter.length, langEnd).trim()
        } else {
            "code"
        }

        val codeContent = if (langEnd in (startIdx + delimiter.length) until endIdx) {
            text.substring(langEnd + 1, endIdx)
        } else {
            text.substring(startIdx + delimiter.length, endIdx)
        }

        list.add(MarkdownBlock(content = codeContent, isCodeBlock = true, language = lang))
        cursor = endIdx + delimiter.length
    }

    return list
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

