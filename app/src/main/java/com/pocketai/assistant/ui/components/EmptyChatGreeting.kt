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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.ui.theme.PocketCardShape
import com.pocketai.assistant.ui.theme.PocketTheme
import com.pocketai.assistant.ui.theme.PillShape

@Composable
fun EmptyChatGreeting(
    assistantName: String,
    onSuggestedPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestedPrompts = listOf(
        "Summarize the main points from my attached document",
        "Explain quantum computing in simple everyday analogies",
        "Help me draft a polite, professional reply to an email",
        "Write a Python script to parse CSV files and compute averages"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pocket AI Avatar Icon
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(PocketTheme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = PocketTheme.colors.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hello! I'm $assistantName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PocketTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(PillShape)
                .background(PocketTheme.colors.cardSecondary)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PocketTheme.colors.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "100% Offline • No Server Calls",
                style = MaterialTheme.typography.labelSmall,
                color = PocketTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Suggested Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PocketTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (prompt in suggestedPrompts) {
                SuggestedPromptCard(
                    prompt = prompt,
                    onClick = { onSuggestedPromptClick(prompt) }
                )
            }
        }
    }
}

@Composable
private fun SuggestedPromptCard(
    prompt: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PocketCardShape)
            .background(PocketTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = PocketTheme.colors.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = PocketTheme.colors.textPrimary,
            fontWeight = FontWeight.Normal
        )
    }
}

