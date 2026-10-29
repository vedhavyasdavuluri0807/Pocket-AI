package com.pocketai.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class OneUiColorPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardSecondary: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primaryVariant: Color,
    val codeBlockBackground: Color,
    val isDark: Boolean
)

val LocalOneUiColors = staticCompositionLocalOf<OneUiColorPalette> {
    error("No OneUiColors provided")
}

private val DarkThemeColors = OneUiColorPalette(
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    cardSecondary = DarkCardSecondary,
    divider = DarkDivider,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    primary = OneUiBluePrimary,
    primaryVariant = OneUiBlueLight,
    codeBlockBackground = CodeBlockDark,
    isDark = true
)

private val LightThemeColors = OneUiColorPalette(
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    cardSecondary = LightCardSecondary,
    divider = LightDivider,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    primary = OneUiBluePrimary,
    primaryVariant = OneUiBlueDark,
    codeBlockBackground = CodeBlockLight,
    isDark = false
)

private val M3DarkColorScheme = darkColorScheme(
    primary = OneUiBluePrimary,
    onPrimary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkDivider
)

private val M3LightColorScheme = lightColorScheme(
    primary = OneUiBluePrimary,
    onPrimary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    outline = LightDivider
)

@Composable
fun PocketAITheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val customColors = if (darkTheme) DarkThemeColors else LightThemeColors
    val m3Colors = if (darkTheme) M3DarkColorScheme else M3LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalOneUiColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            shapes = OneUiShapes,
            typography = OneUiTypography,
            content = content
        )
    }
}

object OneUiTheme {
    val colors: OneUiColorPalette
        @Composable
        get() = LocalOneUiColors.current
}

