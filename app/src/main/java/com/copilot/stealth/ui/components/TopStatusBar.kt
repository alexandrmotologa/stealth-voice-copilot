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
    onToggleWhisper: (() -> Unit)? = null,
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Live audio telemetry
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
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

            Spacer(modifier = Modifier.width(6.dp))
            // Route / Whisper indicator
            val isBt = uiState.isBluetoothHeadsetConnected
            val btPillColor = if (isBt && uiState.isWhisperEnabled) palette.statusActive else if (isBt) palette.highlightAcronym else palette.textMetadata
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isBt) Color(0xFF132328) else Color(0xFF141414))
                    .border(1.dp, if (isBt) Color(0xFF234450) else Color(0xFF222222), RoundedCornerShape(4.dp))
                    .clickable(enabled = isBt && onToggleWhisper != null) { onToggleWhisper?.invoke() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isBt) "🎧 ${if (uiState.isWhisperEnabled) "ON" else "OFF"}" else "MIC:INT",
                    color = btPillColor,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isBt) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Right: Active Engine Selector Pill + Settings trigger
        Row(verticalAlignment = Alignment.CenterVertically) {
            val langCode = uiState.carrierProfile.guidanceLanguage.code
            val langLabel = if (langCode != "OFF") "$langCode➔EN" else "EN"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141A1E))
                    .border(1.dp, Color(0xFF233A48), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text(
                    text = langLabel,
                    color = Color(0xFF80D8FF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val engineLabel = if (uiState.activeEngineType == EngineType.HYBRID) {
                if (uiState.geminiApiKey.isNotBlank()) "GEMINI (P1)"
                else if (uiState.groqApiKey.isNotBlank()) "GROQ (FB1)"
                else "CLOUD (SETUP)"
            } else {
                if (uiState.preferredLocalModel.contains("llama", ignoreCase = true)) "LLAMA" else "GEMMA"
            }
            val engineColor = if (uiState.activeEngineType == EngineType.HYBRID) {
                if (uiState.geminiApiKey.isNotBlank()) Color(0xFF64B5F6) else palette.statusActive
            } else {
                palette.highlightAcronym
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, engineColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onOpenEngineSelection() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(engineColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = engineLabel,
                    color = engineColor,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF222830), RoundedCornerShape(6.dp))
                    .clickable { onOpenTools() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings & Tools",
                    tint = palette.textMetadata,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
