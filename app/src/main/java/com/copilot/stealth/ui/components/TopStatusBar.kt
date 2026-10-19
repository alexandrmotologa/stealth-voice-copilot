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
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.AmoledBlack
import com.copilot.stealth.ui.theme.StatusError
import com.copilot.stealth.ui.theme.StatusListening
import com.copilot.stealth.ui.theme.StatusProcessing
import com.copilot.stealth.ui.theme.StatusStandby
import com.copilot.stealth.ui.theme.TextAnswer
import com.copilot.stealth.ui.theme.TextMetadata
import com.copilot.stealth.ui.theme.TextRomanian

@Composable
fun TopStatusBar(
    uiState: StealthUiState,
    onToggleRomanian: () -> Unit,
    onCycleEngine: () -> Unit,
    onToggleOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        uiState.errorMessage != null -> StatusError
        uiState.isProcessing -> StatusProcessing
        uiState.isListening -> StatusListening
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
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Status indicator dot and label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                    color = TextMetadata,
                    fontSize = 10.sp
                )
            }
        }

        // Action chips: Engine type, Romanian toggle, Overlay toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Engine mode chip
            val engineLabel = when (uiState.activeEngineType) {
                EngineType.HYBRID -> "HYBRID"
                EngineType.DETERMINISTIC_EXPERT -> "EXPERT"
                EngineType.LOCAL_NEURAL -> "NEURAL"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .clickable { onCycleEngine() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = engineLabel,
                    color = TextMetadata,
                    fontSize = 10.sp
                )
            }

            // Romanian translation chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (uiState.isRomanianEnabled) Color(0xFF262618) else Color(0xFF141414))
                    .clickable { onToggleRomanian() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.isRomanianEnabled) "RO: ON" else "RO: OFF",
                    color = if (uiState.isRomanianEnabled) TextRomanian else TextMetadata,
                    fontSize = 10.sp
                )
            }

            // HUD Toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141414))
                    .clickable { onToggleOverlay() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HUD",
                    color = TextAnswer,
                    fontSize = 10.sp
                )
            }
        }
    }
}
