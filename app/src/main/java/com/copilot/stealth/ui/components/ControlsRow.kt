package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.AudioInputSource
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.LocalStealthPalette

@Composable
fun ControlsRow(
    uiState: StealthUiState,
    onToggleListening: () -> Unit,
    onClearHistory: () -> Unit,
    onToggleAudioSource: () -> Unit,
    onRunTestBench: () -> Unit,
    onExportTranscript: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear history button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onClearHistory() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear History",
                tint = palette.textMetadata,
                modifier = Modifier.size(18.dp)
            )
        }

        // Export session transcript button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onExportTranscript() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Export Session",
                tint = palette.textMetadata,
                modifier = Modifier.size(18.dp)
            )
        }

        // Main Mic toggle button
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (uiState.isListening) palette.statusActive else Color(0xFF1A1A1A))
                .clickable { onToggleListening() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Toggle Listening",
                tint = if (uiState.isListening) palette.background else palette.textAnswer,
                modifier = Modifier.size(24.dp)
            )
        }

        // Audio Source selector chip
        val sourceLabel = when (uiState.audioInputSource) {
            AudioInputSource.MICROPHONE -> "SRC: MIC"
            AudioInputSource.SYSTEM_CALL -> "SRC: CALL"
            AudioInputSource.TEST_BENCH -> "SRC: BENCH"
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF141414))
                .clickable { onToggleAudioSource() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = sourceLabel,
                color = palette.textMetadata,
                fontSize = 10.sp
            )
        }

        // Test bench trigger button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onRunTestBench() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Run Test Bench",
                tint = palette.textMetadata,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
