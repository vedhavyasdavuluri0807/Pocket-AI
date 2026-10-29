package com.pocketai.assistant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pocketai.assistant.ui.theme.OneUiTheme
import kotlinx.coroutines.delay

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    val dots = listOf(
        remember { Animatable(0.4f) },
        remember { Animatable(0.4f) },
        remember { Animatable(0.4f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(anim.value)
                    .background(OneUiTheme.colors.primary, shape = CircleShape)
            )
        }
    }
}

