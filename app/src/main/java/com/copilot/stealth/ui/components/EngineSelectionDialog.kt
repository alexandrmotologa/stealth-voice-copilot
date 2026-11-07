package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextOverflow
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 760.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222830))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AI ENGINE SELECTOR",
                                color = palette.textAnswer,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Primary cloud intelligence & offline neural fallback",
                                color = palette.textMetadata,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF181D24))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = palette.textMetadata,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                .background(Color(0xFF0F1713))
                                .border(1.dp, Color(0xFF1D3022), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Column {
                                Text(
                                    text = "AUTOMATIC MULTI-TIER FAILOVER ROUTING",
                                    color = palette.statusActive,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Online: 1. Gemini (P1) ➔ 2. Groq (FB1) ➔ 3. Gemini Fallback ➔ 4. Local Model\nOffline: Instant zero-latency handoff to Local On-Device Neural Model.",
                                    color = Color(0xFFA5B8AD),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

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
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
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
    var selectedPrimaryModel by remember(primaryModel, availableModels) {
        mutableStateOf(if (availableModels.contains(primaryModel)) primaryModel else (availableModels.firstOrNull() ?: primaryModel))
    }
    var selectedFallbackModel by remember(fallbackModel, availableModels) {
        mutableStateOf(if (availableModels.contains(fallbackModel)) fallbackModel else (availableModels.getOrNull(1) ?: fallbackModel))
    }
    var isSavedRecently by remember { mutableStateOf(false) }

    val hasPrimary = primaryApiKey.isNotBlank()
    val isTestedSuccess = (testMessage != null && testMessage.contains("Valid", ignoreCase = true)) ||
            (availableModels.isNotEmpty() && hasPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1520))
            .border(
                1.dp,
                if (hasPrimary) Color(0xFF1E88E5).copy(alpha = 0.6f) else Color(0xFF2A3442),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasPrimary) Color(0xFF2196F3) else Color(0xFFFF5252))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GOOGLE GEMINI (PRIORITY 1)",
                        color = Color.White,
                        fontSize = 12.sp,
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
                    color = if (hasPrimary) Color(0xFF64B5F6) else Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1M+ Token Context • 1,500 RPD Free Tier • aistudio.google.com",
                color = Color(0xFF88A0B8),
                fontSize = 10.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // KEY 1: PRIMARY GEMINI API KEY
            ApiKeyInputField(
                label = "PRIMARY GEMINI API KEY (ACCOUNT 1):",
                placeholder = "AIzaSy... (Account 1)",
                value = inputKey,
                onValueChange = { inputKey = it; isSavedRecently = false },
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) {
                        inputKey = clip
                        isSavedRecently = false
                    }
                },
                onClear = { inputKey = ""; isSavedRecently = false },
                accentColor = Color(0xFF1E88E5)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // KEY 2: BACKUP GEMINI API KEY (FALLBACK)
            ApiKeyInputField(
                label = "BACKUP GEMINI API KEY (ACCOUNT 2 - FALLBACK):",
                placeholder = "AIzaSy... (Account 2 fallback)",
                value = inputBackupKey,
                onValueChange = { inputBackupKey = it; isSavedRecently = false },
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) {
                        inputBackupKey = clip
                        isSavedRecently = false
                    }
                },
                onClear = { inputBackupKey = ""; isSavedRecently = false },
                accentColor = Color(0xFF64B5F6)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fallback triggers automatically if Account 1 hits rate limits (HTTP 429).",
                color = Color(0xFF78909C),
                fontSize = 9.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: TEST & SAVE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TEST & FETCH MODELS BUTTON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13273D))
                        .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isTesting && (inputKey.isNotBlank() || inputBackupKey.isNotBlank())) {
                            val keyToTest = inputKey.ifBlank { inputBackupKey }.trim()
                            onTestKey(keyToTest)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF64B5F6),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TESTING...",
                                color = Color(0xFF64B5F6),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "⚡ TEST & FETCH MODELS",
                            color = Color(0xFF64B5F6),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // SAVE CONFIG BUTTON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSavedRecently) Color(0xFF1E88E5) else Color(0xFF102130))
                        .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(8.dp))
                        .clickable {
                            onSavePrimaryApiKey(inputKey.trim())
                            onSaveBackupApiKey(inputBackupKey.trim())
                            onSavePrimaryModel(selectedPrimaryModel)
                            onSaveFallbackModel(selectedFallbackModel)
                            isSavedRecently = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSavedRecently) "SAVED ✓" else "SAVE CONFIG",
                        color = if (isSavedRecently) Color.White else Color(0xFF90CAF9),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Test Feedback Banner
            if (!testMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isSuccess = testMessage.contains("Valid", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSuccess) Color(0xFF0F2618) else Color(0xFF2C1315))
                        .border(1.dp, if (isSuccess) Color(0xFF00FF66) else Color(0xFFFF5252), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = testMessage,
                        color = if (isSuccess) Color(0xFF00FF66) else Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DYNAMIC MODEL SELECTION: Display ONLY after successful test / valid models
            if (isTestedSuccess && availableModels.isNotEmpty()) {
                Text(
                    text = "AVAILABLE GEMINI MODELS (${availableModels.size}):",
                    color = Color(0xFF64B5F6),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

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
                    label = "FALLBACK MODEL (ON ERROR/QUOTA):",
                    selectedModel = selectedFallbackModel,
                    availableModels = availableModels,
                    accentColor = Color(0xFF64B5F6),
                    onSelectModel = { model ->
                        selectedFallbackModel = model
                        onSaveFallbackModel(model)
                    }
                )
            } else {
                // Locked state placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF090E14))
                        .border(1.dp, Color(0xFF192533), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF546E7A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Model selection unlocks after entering a key and tapping 'TEST & FETCH MODELS'.",
                            color = Color(0xFF607D8B),
                            fontSize = 10.sp
                        )
                    }
                }
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
    var selectedModel by remember(currentModel, availableModels) {
        mutableStateOf(if (availableModels.contains(currentModel)) currentModel else (availableModels.firstOrNull() ?: currentModel))
    }
    var isSavedRecently by remember { mutableStateOf(false) }

    val hasKey = currentApiKey.isNotBlank()
    val isTestedSuccess = (testMessage != null && testMessage.contains("Valid", ignoreCase = true)) ||
            (availableModels.isNotEmpty() && hasKey)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1610))
            .border(
                1.dp,
                if (hasKey) palette.statusActive.copy(alpha = 0.5f) else Color(0xFF223024),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasKey) palette.statusActive else Color(0xFFFF5252))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GROQ CLOUD LPU (FALLBACK 1)",
                        color = palette.textAnswer,
                        fontSize = 12.sp,
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
                    color = if (hasKey) palette.statusActive else Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ultra-Fast LPU Inference • Sub-250ms Response • console.groq.com",
                color = palette.textMetadata,
                fontSize = 10.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // KEY INPUT FIELD
            ApiKeyInputField(
                label = "GROQ API KEY:",
                placeholder = "gsk_... (Groq Console Key)",
                value = inputKey,
                onValueChange = { inputKey = it; isSavedRecently = false },
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) {
                        inputKey = clip
                        isSavedRecently = false
                    }
                },
                onClear = { inputKey = ""; isSavedRecently = false },
                accentColor = palette.statusActive
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: TEST & SAVE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TEST & FETCH MODELS BUTTON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF142618))
                        .border(1.dp, palette.statusActive, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isTesting && inputKey.isNotBlank()) {
                            onTestKey(inputKey.trim())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = palette.statusActive,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TESTING...",
                                color = palette.statusActive,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "⚡ TEST & FETCH MODELS",
                            color = palette.statusActive,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // SAVE CONFIG BUTTON
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSavedRecently) palette.statusActive else Color(0xFF182A1C))
                        .border(1.dp, palette.statusActive, RoundedCornerShape(8.dp))
                        .clickable {
                            onSaveApiKey(inputKey.trim())
                            onSaveModel(selectedModel)
                            isSavedRecently = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSavedRecently) "SAVED ✓" else "SAVE CONFIG",
                        color = if (isSavedRecently) Color.Black else palette.statusActive,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Test Feedback Banner
            if (!testMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isSuccess = testMessage.contains("Valid", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSuccess) Color(0xFF0F2618) else Color(0xFF2C1315))
                        .border(1.dp, if (isSuccess) Color(0xFF00FF66) else Color(0xFFFF5252), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = testMessage,
                        color = if (isSuccess) palette.statusActive else Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DYNAMIC MODEL SELECTION: Display ONLY after successful test / valid models
            if (isTestedSuccess && availableModels.isNotEmpty()) {
                Text(
                    text = "AVAILABLE GROQ MODELS (${availableModels.size}):",
                    color = palette.statusActive,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                ModelDropdownSelector(
                    label = "GROQ MODEL (FAST LPU):",
                    selectedModel = selectedModel,
                    availableModels = availableModels,
                    accentColor = palette.statusActive,
                    onSelectModel = { model ->
                        selectedModel = model
                        onSaveModel(model)
                    }
                )
            } else {
                // Locked state placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A0F0B))
                        .border(1.dp, Color(0xFF1B291E), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF4A6652),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Model selection unlocks after entering a key and tapping 'TEST & FETCH MODELS'.",
                            color = Color(0xFF5A7863),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFF90A4AE),
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (value.isNotBlank()) {
                val preview = if (value.length > 12) {
                    "${value.take(4)}...${value.takeLast(4)} (${value.length} chars)"
                } else {
                    "${value.length} chars"
                }
                Text(
                    text = preview,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070A0E))
                .border(1.dp, Color(0xFF1D2633), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF151F2C))
                    .clickable { onPaste() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = accentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "PASTE",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
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
    val modelsToShow = if (availableModels.isNotEmpty()) availableModels.distinct() else listOf(selectedModel).filter { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF88A0B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070B0E))
                .border(1.dp, if (isExpanded) accentColor else Color(0xFF1E2835), RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedModel.ifBlank { "Select model..." },
                    color = Color.White,
                    fontSize = 11.5.sp,
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
        }

        if (isExpanded && modelsToShow.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F151C))
                    .border(1.dp, Color(0xFF22303E), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp)
            ) {
                modelsToShow.forEach { modelName ->
                    val isCurrent = modelName == selectedModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectModel(modelName)
                                isExpanded = false
                            }
                            .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
    val borderColor = if (isSelected) tagColor else Color(0xFF1E242B)
    val bgColor = if (isSelected) Color(0xFF101814) else Color(0xFF0F1216)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) tagColor.copy(alpha = 0.15f) else Color(0xFF171D24)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) tagColor else Color(0xFF788594),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(tagColor.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
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
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Color(0xFF9EABB8),
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
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
                    modifier = Modifier.size(13.dp)
                )
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
    val borderColor = if (isActive) Color(0xFF00FF66) else Color(0xFF1E242B)
    val bgColor = if (isActive) Color(0xFF0F1712) else Color(0xFF101317)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
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
                    color = Color(0xFF7E8C9A),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B35)),
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
