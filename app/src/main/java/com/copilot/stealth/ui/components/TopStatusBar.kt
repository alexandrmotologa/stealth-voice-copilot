package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.DisplayThemeMode
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.data.models.SttLanguageMode
import com.copilot.stealth.ui.theme.LocalStealthPalette
import com.copilot.stealth.ui.theme.StatusError
import com.copilot.stealth.ui.theme.StatusProcessing
import com.copilot.stealth.ui.theme.StatusStandby
import com.copilot.stealth.ui.theme.StealthPalette

/**
 * 2026 Benchmark AMOLED Top Status Bar featuring live telemetry, bilingual STT selector,
 * whisper earbud toggle, carrier identity access, and theme controls.
 */
@Composable
fun TopStatusBar(
    uiState: StealthUiState,
    onToggleRomanian: () -> Unit,
    onCycleEngine: () -> Unit,
    onCycleSttLanguage: () -> Unit,
    onToggleWhisper: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleCalculator: () -> Unit,
    onToggleCarrierProfile: () -> Unit,
    onToggleMockSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    val statusColor = when {
        uiState.errorMessage != null -> StatusError
        uiState.isProcessing -> StatusProcessing
        uiState.isListening -> palette.statusActive
        else -> StatusStandby
    }

    val statusText = when {
        uiState.errorMessage != null -> "ERROR"
        uiState.isProcessing -> "INFERRING"
        uiState.isListening -> "LISTENING"
        else -> "STANDBY"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Row 1: Telemetry indicators & status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (uiState.lastLatencyMs > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.lastLatencyMs}ms",
                        color = palette.textMetadata,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                // Bluetooth route badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (uiState.isBluetoothHeadsetConnected) Color(0xFF132328) else Color(0xFF161616))
                        .border(1.dp, if (uiState.isBluetoothHeadsetConnected) Color(0xFF234450) else Color(0xFF222222), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isBluetoothHeadsetConnected) "BT: BUDS" else "MIC: INT",
                        color = if (uiState.isBluetoothHeadsetConnected) palette.highlightAcronym else palette.textMetadata,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Engine type badge
            val engineLabel = when (uiState.activeEngineType) {
                EngineType.HYBRID -> "HYBRID"
                EngineType.DETERMINISTIC_EXPERT -> "EXPERT"
                EngineType.LOCAL_NEURAL -> "NEURAL"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF242424), RoundedCornerShape(4.dp))
                    .clickable { onCycleEngine() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = engineLabel,
                    color = palette.textMetadata,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Scrollable quick action chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // STT Language mode chip
            val sttLabel = when (uiState.sttLanguageMode) {
                SttLanguageMode.ENGLISH -> "STT: EN"
                SttLanguageMode.ROMANIAN -> "STT: RO"
                SttLanguageMode.BILINGUAL_AUTO -> "STT: AUTO"
            }
            StatusActionChip(
                text = sttLabel,
                isActive = uiState.sttLanguageMode != SttLanguageMode.ENGLISH,
                activeColor = palette.highlightAcronym,
                palette = palette,
                onClick = onCycleSttLanguage
            )

            // Whisper prompter chip
            StatusActionChip(
                text = if (uiState.isWhisperEnabled) "WHISPER: ON" else "WHISPER: OFF",
                isActive = uiState.isWhisperEnabled,
                activeColor = palette.highlightMoney,
                palette = palette,
                onClick = onToggleWhisper
            )

            // Carrier Profile editor chip
            StatusActionChip(
                text = "PROFILE",
                isActive = uiState.isCarrierProfileOpen,
                activeColor = palette.highlightAcronym,
                palette = palette,
                onClick = onToggleCarrierProfile
            )

            // Mock Simulator chip
            StatusActionChip(
                text = "MOCK SIM",
                isActive = uiState.isMockSimulatorOpen,
                activeColor = palette.highlightTime,
                palette = palette,
                onClick = onToggleMockSimulator
            )

            // Calculator chip
            StatusActionChip(
                text = "CALC",
                isActive = uiState.isCalculatorVisible,
                activeColor = palette.highlightTime,
                palette = palette,
                onClick = onToggleCalculator
            )

            // Theme chip
            val themeLabel = when (uiState.themeMode) {
                DisplayThemeMode.AMOLED_STANDARD -> "THEME: STD"
                DisplayThemeMode.NIGHT_VISION_RED -> "THEME: RED"
                DisplayThemeMode.AMBER_DIFFUSE -> "THEME: AMB"
            }
            StatusActionChip(
                text = themeLabel,
                isActive = false,
                activeColor = palette.textAnswer,
                palette = palette,
                onClick = onCycleTheme
            )

            // Romanian hint toggle
            StatusActionChip(
                text = if (uiState.isRomanianEnabled) "RO: ON" else "RO: OFF",
                isActive = uiState.isRomanianEnabled,
                activeColor = palette.textRomanian,
                palette = palette,
                onClick = onToggleRomanian
            )

            // HUD Toggle
            StatusActionChip(
                text = "HUD",
                isActive = false,
                activeColor = palette.textAnswer,
                palette = palette,
                onClick = onToggleOverlay
            )
        }
    }
}

@Composable
private fun StatusActionChip(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    palette: StealthPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF1C2220) else Color(0xFF141414))
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else Color(0xFF222222),
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isActive) activeColor else palette.textMetadata,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
