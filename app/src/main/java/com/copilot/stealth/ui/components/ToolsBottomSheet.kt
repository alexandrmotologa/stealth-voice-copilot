package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

/**
 * Clean 2026 Tools & Configuration bottom sheet.
 * Consolidates secondary tools (Carrier Profile, Mock Sim, Calculator, Whisper Prompter,
 * Themes, and Developer Benchmarks) into a single organized drawer away from the main dialogue.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolsBottomSheet(
    uiState: StealthUiState,
    onDismiss: () -> Unit,
    onToggleRomanian: () -> Unit,
    onCycleEngine: () -> Unit,
    onCycleSttLanguage: () -> Unit,
    onToggleWhisper: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleCalculator: () -> Unit,
    onOpenCarrierProfile: () -> Unit,
    onOpenMockSimulator: () -> Unit,
    onOpenEngineSelection: (() -> Unit)? = null,
    onScenarioSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = LocalStealthPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
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
                        text = "SETTINGS & TOOLS",
                        color = palette.textAnswer,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Customize carrier data, whisper audio & simulation",
                        color = palette.textMetadata,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.textMetadata,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Core Navigation Tools
            Text(
                text = "INTERVIEW MODULES",
                color = palette.statusActive,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolMenuButton(
                    title = "PROFILE & CONTEXT",
                    subtitle = "Resume, prompt & mode",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onOpenCarrierProfile()
                    }
                )

                ToolMenuButton(
                    title = "MOCK SIMULATOR",
                    subtitle = "Practice scenarios",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onOpenMockSimulator()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolMenuButton(
                    title = if (uiState.isCalculatorVisible) "HIDE CALCULATOR" else "FREIGHT MATH BAR",
                    subtitle = "Detention & RPM rates",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onToggleCalculator()
                    }
                )

                val engineLabel = when (uiState.activeEngineType) {
                    EngineType.HYBRID -> "ENGINE: HYBRID (AI)"
                    EngineType.DETERMINISTIC_EXPERT -> "ENGINE: EXPERT"
                    EngineType.LOCAL_NEURAL -> "ENGINE: NEURAL"
                }
                val engineSub = when (uiState.activeEngineType) {
                    EngineType.HYBRID -> "Ultra-fast AI (Sub-250ms)"
                    EngineType.DETERMINISTIC_EXPERT -> "Local Deterministic"
                    EngineType.LOCAL_NEURAL -> "On-Device Neural"
                }
                ToolMenuButton(
                    title = engineLabel,
                    subtitle = engineSub,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (onOpenEngineSelection != null) {
                            onOpenEngineSelection()
                        } else {
                            onCycleEngine()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Stealth Preferences
            Text(
                text = "STEALTH PREFERENCES",
                color = palette.statusActive,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Whisper prompter switch
            PreferenceToggleRow(
                title = "Whisper Earpiece Prompter",
                subtitle = "Reads answers into Bluetooth earbuds with zero speaker leak",
                checked = uiState.isWhisperEnabled,
                onCheckedChange = { onToggleWhisper() }
            )

            // Romanian guidance switch
            PreferenceToggleRow(
                title = "Romanian Guidance (RO)",
                subtitle = "Displays Romanian translation and interview coaching",
                checked = uiState.isRomanianEnabled,
                onCheckedChange = { onToggleRomanian() }
            )

            // Theme selector row
            val themeName = when (uiState.themeMode) {
                DisplayThemeMode.AMOLED_STANDARD -> "AMOLED True Black"
                DisplayThemeMode.NIGHT_VISION_RED -> "Night Vision Red"
                DisplayThemeMode.AMBER_DIFFUSE -> "Amber Diffuse"
            }
            PreferenceClickableRow(
                title = "Display Theme",
                currentValue = themeName,
                onClick = onCycleTheme
            )

            // STT Language
            val sttName = when (uiState.sttLanguageMode) {
                SttLanguageMode.ENGLISH -> "English Only (en-US)"
                SttLanguageMode.ROMANIAN -> "Romanian (ro-RO)"
                SttLanguageMode.BILINGUAL_AUTO -> "Bilingual Auto"
            }
            PreferenceClickableRow(
                title = "Speech Recognition",
                currentValue = sttName,
                onClick = onCycleSttLanguage
            )

            // Floating HUD toggle
            PreferenceClickableRow(
                title = "Window Mode",
                currentValue = uiState.overlayMode.name,
                onClick = onToggleOverlay
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Scenario Benchmarks (Tucked away for quick testing)
            Text(
                text = "TEST BENCHMARKS (TAP TO SIMULATE)",
                color = palette.textMetadata,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val benchmarks = listOf(
                    "Detention" to "What is your standard policy when a driver is delayed at a facility past two hours?",
                    "TONU" to "The broker cancels the load while the driver is already on site. What is your TONU fee?",
                    "Rate Con" to "What do you verify on a rate confirmation before signing?",
                    "Layover" to "What is your standard layover policy if the facility holds the driver overnight?",
                    "HOS & ELD" to "Explain property-carrying Hours of Service regulations and how you plan long haul transit.",
                    "Lumper" to "The receiver demands a cash lumper payment. What is your policy?",
                    "OS&D BOL" to "What should the driver do if the cargo count does not match the BOL at pickup?"
                )

                benchmarks.forEach { (label, query) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF242424), RoundedCornerShape(6.dp))
                            .clickable {
                                onDismiss()
                                onScenarioSelected(query)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = palette.textMetadata,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolMenuButton(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalStealthPalette.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = palette.textAnswer,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = palette.textMetadata,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalStealthPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textAnswer,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textMetadata,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.statusActive,
                uncheckedThumbColor = palette.textMetadata,
                uncheckedTrackColor = Color(0xFF1E1E1E)
            )
        )
    }
}

@Composable
private fun PreferenceClickableRow(
    title: String,
    currentValue: String,
    onClick: () -> Unit
) {
    val palette = LocalStealthPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = palette.textAnswer,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "$currentValue ❯",
            color = palette.statusActive,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
