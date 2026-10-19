package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.DisplayThemeMode
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.LocalStealthPalette
import com.copilot.stealth.ui.theme.StatusError
import com.copilot.stealth.ui.theme.StatusListening
import com.copilot.stealth.ui.theme.StatusProcessing
import com.copilot.stealth.ui.theme.StatusStandby

@Composable
fun TopStatusBar(
    uiState: StealthUiState,
    onToggleRomanian: () -> Unit,
    onCycleEngine: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleCalculator: () -> Unit,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Status indicator dot and label + BT route badge
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
                letterSpacing = 1.sp
            )
            if (uiState.lastLatencyMs > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${uiState.lastLatencyMs}ms",
                    color = palette.textMetadata,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            // Bluetooth audio route badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (uiState.isBluetoothHeadsetConnected) Color(0xFF132328) else Color(0xFF1A1A1A))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (uiState.isBluetoothHeadsetConnected) "BT: EARPIECE" else "MIC: PHONE",
                    color = if (uiState.isBluetoothHeadsetConnected) palette.highlightAcronym else palette.textMetadata,
                    fontSize = 9.sp
                )
            }
        }

        // Action chips: Theme, Calc, Engine type, Romanian toggle, Overlay toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Theme mode chip
            val themeLabel = when (uiState.themeMode) {
                DisplayThemeMode.AMOLED_STANDARD -> "STD"
                DisplayThemeMode.NIGHT_VISION_RED -> "RED"
                DisplayThemeMode.AMBER_DIFFUSE -> "AMB"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .clickable { onCycleTheme() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = themeLabel,
                    color = palette.textAnswer,
                    fontSize = 10.sp
                )
            }

            // Calculator toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (uiState.isCalculatorVisible) Color(0xFF2B2610) else Color(0xFF141414))
                    .clickable { onToggleCalculator() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CALC",
                    color = if (uiState.isCalculatorVisible) palette.highlightTime else palette.textMetadata,
                    fontSize = 10.sp
                )
            }

            // Engine mode chip
            val engineLabel = when (uiState.activeEngineType) {
                EngineType.HYBRID -> "HYB"
                EngineType.DETERMINISTIC_EXPERT -> "EXP"
                EngineType.LOCAL_NEURAL -> "NEU"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .clickable { onCycleEngine() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = engineLabel,
                    color = palette.textMetadata,
                    fontSize = 10.sp
                )
            }

            // Romanian translation chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (uiState.isRomanianEnabled) Color(0xFF262618) else Color(0xFF141414))
                    .clickable { onToggleRomanian() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.isRomanianEnabled) "RO" else "EN",
                    color = if (uiState.isRomanianEnabled) palette.textRomanian else palette.textMetadata,
                    fontSize = 10.sp
                )
            }

            // HUD Toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .clickable { onToggleOverlay() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HUD",
                    color = palette.textAnswer,
                    fontSize = 10.sp
                )
            }
        }
    }
}
