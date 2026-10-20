package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.LocalStealthPalette
import com.copilot.stealth.ui.theme.StatusError
import com.copilot.stealth.ui.theme.StatusProcessing
import com.copilot.stealth.ui.theme.StatusStandby

/**
 * Minimalist AMOLED Top Status Bar.
 * Clean, discreet telemetry displaying voice activity status, latency,
 * active AI Engine selector, and settings trigger.
 */
@Composable
fun TopStatusBar(
    uiState: StealthUiState,
    onOpenTools: () -> Unit,
    onOpenEngineSelection: () -> Unit,
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
        uiState.isProcessing -> "PROMPTER: AI"
        uiState.isListening -> "PROMPTER: LIVE"
        else -> "STANDBY"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Live audio telemetry
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
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (uiState.lastLatencyMs > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${uiState.lastLatencyMs}ms",
                    color = palette.textMetadata,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            // Route indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (uiState.isBluetoothHeadsetConnected) Color(0xFF132328) else Color(0xFF141414))
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

        // Right: Active Engine Selector Pill + Settings trigger
        Row(verticalAlignment = Alignment.CenterVertically) {
            val engineLabel = if (uiState.activeEngineType == EngineType.HYBRID) {
                "GROQ (AI)"
            } else {
                if (uiState.preferredLocalModel.contains("llama", ignoreCase = true)) "LLAMA" else "GEMMA"
            }
            val engineColor = if (uiState.activeEngineType == EngineType.HYBRID) {
                palette.statusActive
            } else {
                palette.highlightAcronym
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, engineColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onOpenEngineSelection() }
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(engineColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = engineLabel,
                    color = engineColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
                    .clickable { onOpenTools() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings & Tools",
                    tint = palette.textMetadata,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SETTINGS",
                    color = palette.textMetadata,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
