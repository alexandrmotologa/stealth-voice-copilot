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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.LocalStealthPalette

/**
 * 2026 Minimalist Controls Row.
 * Focuses 100% on the core action: Start/Stop listening, with discreet clear, export, and settings.
 */
@Composable
fun ControlsRow(
    uiState: StealthUiState,
    onToggleListening: () -> Unit,
    onClearHistory: () -> Unit,
    onExportTranscript: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear history button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onClearHistory() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear History",
                tint = palette.textMetadata,
                modifier = Modifier.size(20.dp)
            )
        }

        // Export session transcript button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onExportTranscript() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Export Session",
                tint = palette.textMetadata,
                modifier = Modifier.size(20.dp)
            )
        }

        // Main Mic toggle button (Prominent, High-Fidelity)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (uiState.isListening) palette.statusActive else Color(0xFF1A1A1A))
                .clickable { onToggleListening() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Toggle Listening",
                tint = if (uiState.isListening) palette.background else palette.textAnswer,
                modifier = Modifier.size(28.dp)
            )
        }

        // Settings / Tools drawer trigger
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .clickable { onOpenTools() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings & Tools",
                tint = palette.textMetadata,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
