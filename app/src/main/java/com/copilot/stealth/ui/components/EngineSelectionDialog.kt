package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.theme.LocalStealthPalette

@Composable
fun EngineSelectionDialog(
    uiState: StealthUiState,
    onSelectEngine: (EngineType) -> Unit,
    onSelectLocalModel: (String) -> Unit,
    onTriggerDownload: (String) -> Unit,
    onSaveGroqApiKey: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val palette = LocalStealthPalette.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI ENGINE SELECTOR",
                            color = palette.textAnswer,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Choose primary inference & offline fallback",
                            color = palette.textMetadata,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textMetadata
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Primary Engine
                Text(
                    text = "PRIMARY INFERENCE MODE",
                    color = palette.statusActive,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Groq Cloud AI
                val isGroqSelected = uiState.activeEngineType == EngineType.HYBRID
                EngineCard(
                    title = "Groq Cloud AI (Ultra-fast)",
                    subtitle = "Sub-250ms latency • Qwen 27B AI • Free Tier",
                    tag = "RECOMMENDED",
                    tagColor = palette.statusActive,
                    icon = Icons.Default.CloudQueue,
                    isSelected = isGroqSelected,
                    onClick = { onSelectEngine(EngineType.HYBRID) }
                )

                if (isGroqSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GroqApiKeyConfigCard(
                        currentApiKey = uiState.groqApiKey,
                        onSaveApiKey = onSaveGroqApiKey
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Local Neural Model
                val isLocalSelected = uiState.activeEngineType == EngineType.LOCAL_NEURAL
                EngineCard(
                    title = "Local Neural AI (On-Device)",
                    subtitle = "100% Offline • Private • Runs on Tensor/Snapdragon",
                    tag = "OFFLINE",
                    tagColor = palette.highlightAcronym,
                    icon = Icons.Default.Memory,
                    isSelected = isLocalSelected,
                    onClick = { onSelectEngine(EngineType.LOCAL_NEURAL) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Local Model Selection & Download
                Text(
                    text = "LOCAL MODEL / OFFLINE FALLBACK",
                    color = palette.statusActive,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Always ready when offline or in local mode. Both can be installed:",
                    color = palette.textMetadata,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                // Model 1: Llama 3.2 1B
                LocalModelRow(
                    name = "Llama 3.2 1B Instruct",
                    archDesc = "Meta Llama • Fast & Conversational (~1.2 GB)",
                    isDownloaded = uiState.isLlamaDownloaded,
                    isActive = uiState.preferredLocalModel.contains("llama", ignoreCase = true),
                    onSelect = { onSelectLocalModel("Llama 3.2 1B") },
                    onDownload = { onTriggerDownload("llama_3_2_1b") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Model 2: Gemma 2 2B
                LocalModelRow(
                    name = "Gemma 2 2B IT",
                    archDesc = "Google Gemma • Advanced Reasoning (~1.35 GB)",
                    isDownloaded = uiState.isGemmaDownloaded,
                    isActive = uiState.preferredLocalModel.contains("gemma", ignoreCase = true),
                    onSelect = { onSelectLocalModel("Gemma 2 2B") },
                    onDownload = { onTriggerDownload("gemma_2_2b") }
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.statusActive)
                ) {
                    Text(
                        text = "CONFIRM & APPLY",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) tagColor else Color(0xFF1E1E1E)
    val bgColor = if (isSelected) Color(0xFF111713) else Color(0xFF111111)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) tagColor else Color(0xFF888888),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(tagColor.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tag,
                            color = tagColor,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF9E9E9E),
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(tagColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalModelRow(
    name: String,
    archDesc: String,
    isDownloaded: Boolean,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit
) {
    val borderColor = if (isActive) Color(0xFF00FF66) else Color(0xFF1E1E1E)
    val bgColor = if (isActive) Color(0xFF0F1510) else Color(0xFF101010)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[ACTIVE]",
                            color = Color(0xFF00FF66),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = archDesc,
                    color = Color(0xFF7E7E7E),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isDownloaded) Color(0xFF00FF66) else Color(0xFFFF9900))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDownloaded) "Ready on device" else "Not downloaded yet",
                        color = if (isDownloaded) Color(0xFF00FF66) else Color(0xFFFF9900),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isDownloaded) {
                if (!isActive) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "USE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2E20)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GET",
                        color = Color(0xFF00FF66),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GroqApiKeyConfigCard(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit
) {
    val palette = LocalStealthPalette.current
    val clipboardManager = LocalClipboardManager.current
    var inputKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var isSavedRecently by remember { mutableStateOf(false) }

    val hasKey = currentApiKey.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C120E))
            .border(
                1.dp,
                if (hasKey) palette.statusActive.copy(alpha = 0.5f) else Color(0xFF382222),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (hasKey) palette.statusActive else Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GROQ API KEY",
                        color = palette.textAnswer,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (hasKey) {
                        "● ACTIVE (${currentApiKey.take(4)}...${currentApiKey.takeLast(4)})"
                    } else {
                        "○ MISSING KEY"
                    },
                    color = if (hasKey) palette.statusActive else Color(0xFFE53935),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF050505))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (inputKey.isEmpty()) {
                    Text(
                        text = "gsk_...",
                        color = Color(0xFF555555),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        isSavedRecently = false
                    },
                    textStyle = TextStyle(
                        color = palette.textAnswer,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(palette.statusActive),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row: Paste & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paste button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                        .clickable {
                            val clipText = clipboardManager.getText()?.text?.trim()
                            if (!clipText.isNullOrBlank()) {
                                inputKey = clipText
                                isSavedRecently = false
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PASTE FROM CLIPBOARD",
                        color = palette.textMetadata,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Save button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSavedRecently) palette.statusActive else Color(0xFF1C2D1F))
                        .border(1.dp, palette.statusActive, RoundedCornerShape(6.dp))
                        .clickable {
                            val trimmed = inputKey.trim()
                            onSaveApiKey(trimmed)
                            isSavedRecently = true
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSavedRecently) "SAVED ✓" else "SAVE KEY",
                        color = if (isSavedRecently) Color.Black else palette.statusActive,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Free API key from console.groq.com. Stored locally on device, never sent to GitHub.",
                color = palette.textMetadata,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
    }
}
