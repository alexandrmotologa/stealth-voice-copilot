package com.copilot.stealth.ui.theme

import androidx.compose.ui.graphics.Color
import com.copilot.stealth.data.models.DisplayThemeMode

// Baseline AMOLED Palette
val AmoledBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF080808)
val DarkBorder = Color(0xFF181818)

val TextQuestion = Color(0xFF6E6E6E)
val TextAnswer = Color(0xFFD4D4D4)
val TextRomanian = Color(0xFF8E8E78)
val TextMetadata = Color(0xFF505050)

val StatusListening = Color(0xFF3DDC84)
val StatusProcessing = Color(0xFFFFA726)
val StatusStandby = Color(0xFF424242)
val StatusError = Color(0xFFE57373)

data class StealthPalette(
    val background: Color,
    val surface: Color,
    val textAnswer: Color,
    val textQuestion: Color,
    val textRomanian: Color,
    val textMetadata: Color,
    val highlightMoney: Color,
    val highlightAcronym: Color,
    val highlightTime: Color,
    val statusActive: Color
)

object StealthPalettes {
    val StandardAmoled = StealthPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0C),
        textAnswer = Color(0xFFD4D4D4),
        textQuestion = Color(0xFF6E6E6E),
        textRomanian = Color(0xFF8E8E78),
        textMetadata = Color(0xFF505050),
        highlightMoney = Color(0xFF81C784),
        highlightAcronym = Color(0xFF80D8FF),
        highlightTime = Color(0xFFFFD54F),
        statusActive = Color(0xFF3DDC84)
    )

    val NightVisionRed = StealthPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0D0303),
        textAnswer = Color(0xFFA62828), // Deep dim crimson
        textQuestion = Color(0xFF5A1414),
        textRomanian = Color(0xFF701C1C),
        textMetadata = Color(0xFF4A0F0F),
        highlightMoney = Color(0xFFCC3333),
        highlightAcronym = Color(0xFFE64D4D),
        highlightTime = Color(0xFFB82424),
        statusActive = Color(0xFFD32F2F)
    )

    val AmberDiffuse = StealthPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0C0902),
        textAnswer = Color(0xFFB88A30), // Muted dark amber
        textQuestion = Color(0xFF5C4518),
        textRomanian = Color(0xFF785B22),
        textMetadata = Color(0xFF473612),
        highlightMoney = Color(0xFFDCA83D),
        highlightAcronym = Color(0xFFF0BE54),
        highlightTime = Color(0xFFC7952E),
        statusActive = Color(0xFFFFA000)
    )

    fun get(mode: DisplayThemeMode): StealthPalette {
        return when (mode) {
            DisplayThemeMode.AMOLED_STANDARD -> StandardAmoled
            DisplayThemeMode.NIGHT_VISION_RED -> NightVisionRed
            DisplayThemeMode.AMBER_DIFFUSE -> AmberDiffuse
        }
    }
}
