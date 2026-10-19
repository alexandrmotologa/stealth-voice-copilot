package com.copilot.stealth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TextAnswer,
    secondary = TextRomanian,
    background = AmoledBlack,
    surface = AmoledBlack,
    onPrimary = AmoledBlack,
    onSecondary = AmoledBlack,
    onBackground = TextAnswer,
    onSurface = TextAnswer
)

@Composable
fun StealthVoiceCopilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = StealthTypography,
        content = content
    )
}
