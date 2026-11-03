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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onSaveGroqModel: (String) -> Unit = {},
    onTestGroqKey: (String) -> Unit = {},
    onSaveGeminiApiKey: (String) -> Unit = {},
    onSaveGeminiBackupApiKey: (String) -> Unit = {},
    onSaveGeminiModel: (String) -> Unit = {},
    onSaveGeminiFallbackModel: (String) -> Unit = {},
    onTestGeminiKey: (String) -> Unit = {},
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

                // Option 1: Cloud AI (Gemini Priority 1 + Groq Fallback)
                val isHybridSelected = uiState.activeEngineType == EngineType.HYBRID
                EngineCard(
                    title = "Cloud AI (Gemini Priority + Groq Fallback)",
                    subtitle = "Gemini 1M Context (P1) • Groq LPU Sub-250ms • Multi-tier Failover",
                    tag = "RECOMMENDED",
                    tagColor = palette.statusActive,
                    icon = Icons.Default.CloudQueue,
                    isSelected = isHybridSelected,
                    onClick = { onSelectEngine(EngineType.HYBRID) }
                )

                if (isHybridSelected) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority Fallback Hierarchy Visual Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1410))
                            .border(1.dp, Color(0xFF1B2A1E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "AUTOMATIC MULTI-TIER FALLBACK ROUTING",
                                color = palette.statusActive,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Online: 1. Gemini (P1) ➔ 2. Groq (FB1) ➔ 3. Gemini Fallback ➔ 4. Local Model\nOffline: Instant zero-latency handoff to Local On-Device Neural Model.",
                                color = palette.textMetadata,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority 1: Gemini (Google AI Studio)
                    GeminiApiKeyConfigCard(
                        primaryApiKey = uiState.geminiApiKey,
                        backupApiKey = uiState.geminiBackupApiKey,
                        primaryModel = uiState.geminiModel,
                        fallbackModel = uiState.geminiFallbackModel,
                        availableModels = uiState.availableGeminiModels,
                        isTesting = uiState.isTestingGeminiKey,
                        testMessage = uiState.geminiKeyTestMessage,
                        onSavePrimaryApiKey = onSaveGeminiApiKey,
                        onSaveBackupApiKey = onSaveGeminiBackupApiKey,
                        onSavePrimaryModel = onSaveGeminiModel,
                        onSaveFallbackModel = onSaveGeminiFallbackModel,
                        onTestKey = onTestGeminiKey
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fallback 1: Groq Cloud LPU
                    GroqApiKeyConfigCard(
                        currentApiKey = uiState.groqApiKey,
                        currentModel = uiState.groqModel,
                        availableModels = uiState.availableGroqModels,
                        isTesting = uiState.isTestingGroqKey,
                        testMessage = uiState.groqKeyTestMessage,
                        onSaveApiKey = onSaveGroqApiKey,
                        onSaveModel = onSaveGroqModel,
                        onTestKey = onTestGroqKey
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
private fun ModelDropdownSelector(
    label: String,
    selectedModel: String,
    availableModels: List<String>,
    accentColor: Color,
    onSelectModel: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF88A0B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF070B0E))
                .border(1.dp, if (isExpanded) accentColor else Color(0xFF1E262E), RoundedCornerShape(6.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedModel.ifBlank { "Select model..." },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier
                    .background(Color(0xFF0F151C))
                    .border(1.dp, Color(0xFF22303E), RoundedCornerShape(8.dp))
            ) {
                val modelsToShow = if (availableModels.isNotEmpty()) availableModels else listOf(selectedModel)
                modelsToShow.distinct().forEach { modelName ->
                    val isCurrent = modelName == selectedModel
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modelName,
                                    color = if (isCurrent) accentColor else Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectModel(modelName)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiApiKeyConfigCard(
    primaryApiKey: String,
    backupApiKey: String,
    primaryModel: String,
    fallbackModel: String,
    availableModels: List<String>,
    isTesting: Boolean,
    testMessage: String?,
    onSavePrimaryApiKey: (String) -> Unit,
    onSaveBackupApiKey: (String) -> Unit,
    onSavePrimaryModel: (String) -> Unit,
    onSaveFallbackModel: (String) -> Unit,
    onTestKey: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var inputKey by remember(primaryApiKey) { mutableStateOf(primaryApiKey) }
    var inputBackupKey by remember(backupApiKey) { mutableStateOf(backupApiKey) }
    var selectedPrimaryModel by remember(primaryModel) { mutableStateOf(primaryModel) }
    var selectedFallbackModel by remember(fallbackModel) { mutableStateOf(fallbackModel) }
    var isSavedRecently by remember { mutableStateOf(false) }

    val hasPrimary = primaryApiKey.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B1218))
            .border(
                1.dp,
                if (hasPrimary) Color(0xFF1E88E5).copy(alpha = 0.6f) else Color(0xFF382222),
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
                            .background(if (hasPrimary) Color(0xFF1E88E5) else Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GOOGLE GEMINI (PRIORITY 1)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (hasPrimary) {
                        "● READY (${primaryApiKey.take(4)}...)"
                    } else {
                        "○ NOT SET"
                    },
                    color = if (hasPrimary) Color(0xFF64B5F6) else Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1M+ Token Context • 1,500 RPD Free Tier • aistudio.google.com",
                color = Color(0xFF88A0B8),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Key Field Label & Input
            Text(
                text = "PRIMARY GEMINI API KEY:",
                color = Color(0xFF7A8D9E),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF05080C))
                    .border(1.dp, Color(0xFF1A2633), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                if (inputKey.isEmpty()) {
                    Text(text = "AIzaSy...", color = Color(0xFF4A5D6E), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                BasicTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it; isSavedRecently = false },
                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Color(0xFF1E88E5)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Test Key & Fetch Models Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF132230))
                        .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(5.dp))
                        .clickable(enabled = !isTesting && inputKey.isNotBlank()) {
                            onTestKey(inputKey.trim())
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF64B5F6), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "TESTING KEY...", color = Color(0xFF64B5F6), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "⚡ TEST KEY & FETCH MODELS",
                            color = Color(0xFF64B5F6),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Paste button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0F1822))
                        .border(1.dp, Color(0xFF223548), RoundedCornerShape(5.dp))
                        .clickable {
                            val clipText = clipboardManager.getText()?.text?.trim()
                            if (!clipText.isNullOrBlank()) {
                                inputKey = clipText
                                isSavedRecently = false
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "PASTE", color = Color(0xFF90CAF9), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Test Message Output
            if (!testMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val isSuccess = testMessage.contains("Valid", ignoreCase = true)
                Text(
                    text = testMessage,
                    color = if (isSuccess) Color(0xFF00FF66) else Color(0xFFFF5252),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Model Dropdown
            ModelDropdownSelector(
                label = "PRIMARY MODEL (DEFAULT):",
                selectedModel = selectedPrimaryModel,
                availableModels = availableModels,
                accentColor = Color(0xFF1E88E5),
                onSelectModel = { model ->
                    selectedPrimaryModel = model
                    onSavePrimaryModel(model)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fallback Model Dropdown
            ModelDropdownSelector(
                label = "FALLBACK MODEL (ON ERROR):",
                selectedModel = selectedFallbackModel,
                availableModels = availableModels,
                accentColor = Color(0xFF64B5F6),
                onSelectModel = { model ->
                    selectedFallbackModel = model
                    onSaveFallbackModel(model)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Optional Backup Key Field
            Text(
                text = "BACKUP GEMINI API KEY (OPTIONAL - DOUBLE QUOTA):",
                color = Color(0xFF6B7E90),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF05080C))
                    .border(1.dp, Color(0xFF1A2633), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (inputBackupKey.isEmpty()) {
                    Text(text = "Optional 2nd account key...", color = Color(0xFF394652), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                BasicTextField(
                    value = inputBackupKey,
                    onValueChange = { inputBackupKey = it; isSavedRecently = false },
                    textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Color(0xFF1E88E5)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Gemini Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSavedRecently) Color(0xFF1E88E5) else Color(0xFF0D2840))
                    .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(6.dp))
                    .clickable {
                        onSavePrimaryApiKey(inputKey.trim())
                        onSaveBackupApiKey(inputBackupKey.trim())
                        onSavePrimaryModel(selectedPrimaryModel)
                        onSaveFallbackModel(selectedFallbackModel)
                        isSavedRecently = true
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSavedRecently) "SAVED GEMINI CONFIG ✓" else "SAVE GEMINI CONFIG",
                    color = if (isSavedRecently) Color.White else Color(0xFF64B5F6),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GroqApiKeyConfigCard(
    currentApiKey: String,
    currentModel: String,
    availableModels: List<String>,
    isTesting: Boolean,
    testMessage: String?,
    onSaveApiKey: (String) -> Unit,
    onSaveModel: (String) -> Unit,
    onTestKey: (String) -> Unit
) {
    val palette = LocalStealthPalette.current
    val clipboardManager = LocalClipboardManager.current
    var inputKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
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
                        text = "GROQ CLOUD LPU (FALLBACK 1)",
                        color = palette.textAnswer,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (hasKey) {
                        "● READY (${currentApiKey.take(4)}...)"
                    } else {
                        "○ OPTIONAL"
                    },
                    color = if (hasKey) palette.statusActive else Color(0xFFE53935),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Flagship Free: llama-3.3-70b-versatile (Smartest Free) & llama-3.1-8b-instant (Fastest)",
                color = palette.textMetadata,
                fontSize = 9.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Key Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF050505))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                if (inputKey.isEmpty()) {
                    Text(text = "gsk_...", color = Color(0xFF555555), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                BasicTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it; isSavedRecently = false },
                    textStyle = TextStyle(color = palette.textAnswer, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(palette.statusActive),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Test Key Button & Paste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF142418))
                        .border(1.dp, palette.statusActive, RoundedCornerShape(5.dp))
                        .clickable(enabled = !isTesting && inputKey.isNotBlank()) {
                            onTestKey(inputKey.trim())
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = palette.statusActive, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "TESTING KEY...", color = palette.statusActive, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "⚡ TEST KEY & FETCH MODELS",
                            color = palette.statusActive,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(5.dp))
                        .clickable {
                            val clipText = clipboardManager.getText()?.text?.trim()
                            if (!clipText.isNullOrBlank()) {
                                inputKey = clipText
                                isSavedRecently = false
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "PASTE", color = palette.textMetadata, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Test Message
            if (!testMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val isSuccess = testMessage.contains("Valid", ignoreCase = true)
                Text(
                    text = testMessage,
                    color = if (isSuccess) palette.statusActive else Color(0xFFFF5252),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Groq Model Dropdown Selector
            ModelDropdownSelector(
                label = "GROQ MODEL VARIANT (FREE PRODUCTION TIER):",
                selectedModel = selectedModel,
                availableModels = availableModels,
                accentColor = palette.statusActive,
                onSelectModel = { model ->
                    selectedModel = model
                    onSaveModel(model)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Save Groq Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSavedRecently) palette.statusActive else Color(0xFF1C2D1F))
                    .border(1.dp, palette.statusActive, RoundedCornerShape(6.dp))
                    .clickable {
                        onSaveApiKey(inputKey.trim())
                        onSaveModel(selectedModel)
                        isSavedRecently = true
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSavedRecently) "SAVED GROQ CONFIG ✓" else "SAVE GROQ CONFIG",
                    color = if (isSavedRecently) Color.Black else palette.statusActive,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
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
