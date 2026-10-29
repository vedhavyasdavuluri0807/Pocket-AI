package com.pocketai.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),       // Signature Samsung One UI Card radius
    extraLarge = RoundedCornerShape(32.dp)
)

val PillShape = RoundedCornerShape(50.dp)
val BubbleUserShape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)
val BubbleAssistantShape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
val OneUiCardShape = RoundedCornerShape(26.dp)
val OneUiSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

