package com.copilot.stealth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.copilot.stealth.data.models.DisplayThemeMode

val LocalStealthPalette = staticCompositionLocalOf { StealthPalettes.StandardAmoled }

@Composable
fun StealthVoiceCopilotTheme(
    themeMode: DisplayThemeMode = DisplayThemeMode.AMOLED_STANDARD,
    content: @Composable () -> Unit
) {
    val palette = StealthPalettes.get(themeMode)

    val colorScheme = darkColorScheme(
        primary = palette.textAnswer,
        secondary = palette.textRomanian,
        background = palette.background,
        surface = palette.surface,
        onPrimary = palette.background,
        onSecondary = palette.background,
        onBackground = palette.textAnswer,
        onSurface = palette.textAnswer
    )

    CompositionLocalProvider(LocalStealthPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StealthTypography,
            content = content
        )
    }
}
