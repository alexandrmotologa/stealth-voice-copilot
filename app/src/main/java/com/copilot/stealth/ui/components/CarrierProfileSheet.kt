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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.ProfileMode
import com.copilot.stealth.data.models.TeleprompterFontSize
import com.copilot.stealth.ui.theme.StealthPalette

/**
 * AI Teleprompter Profile & Prompt Configuration Sheet.
 * Allows switching between Global Free mode, Candidate Interview mode, Custom Prompting,
 * and Freight Logistics, with local on-device persistence (never committed to git).
 */
@Composable
fun CarrierProfileSheet(
    currentProfile: CarrierProfile,
    palette: StealthPalette,
    onSaveProfile: (CarrierProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(currentProfile.profileMode) }
    var selectedFontSize by remember { mutableStateOf(currentProfile.fontSize) }
    var candidateContext by remember { mutableStateOf(currentProfile.candidateContext) }
    var customPrompt by remember { mutableStateOf(currentProfile.customPrompt) }

    // Freight fields
    var companyName by remember { mutableStateOf(currentProfile.companyName) }
    var mcNumber by remember { mutableStateOf(currentProfile.mcNumber) }
    var dotNumber by remember { mutableStateOf(currentProfile.dotNumber) }
    var fleetSize by remember { mutableStateOf(currentProfile.fleetSize.toString()) }
    var equipment by remember { mutableStateOf(currentProfile.equipmentTypes) }
    var factoring by remember { mutableStateOf(currentProfile.factoringCompany) }
    var detentionRate by remember { mutableStateOf(currentProfile.standardDetentionRate.toString()) }
    var tonuFee by remember { mutableStateOf(currentProfile.standardTonuFee.toString()) }
    var homeBase by remember { mutableStateOf(currentProfile.homeBase) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(palette.surface)
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF333333))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI PROMPTER PROFILE & CONTEXT",
                    color = palette.textAnswer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Private to this device | Stored in local SharedPreferences",
                    color = palette.textMetadata,
                    fontSize = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onDismiss() }
                    .padding(6.dp)
            ) {
                Text("✕", color = palette.textMetadata, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Mode Selection
        Text(
            text = "OPERATION MODE",
            color = palette.highlightAcronym,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModeSelectChip(
                label = "GLOBAL FREE",
                isSelected = selectedMode == ProfileMode.GENERAL_CONVERSATIONAL,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedMode = ProfileMode.GENERAL_CONVERSATIONAL }

            ModeSelectChip(
                label = "CANDIDATE",
                isSelected = selectedMode == ProfileMode.CANDIDATE_INTERVIEW,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedMode = ProfileMode.CANDIDATE_INTERVIEW }

            ModeSelectChip(
                label = "CUSTOM",
                isSelected = selectedMode == ProfileMode.CUSTOM_PROMPT,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedMode = ProfileMode.CUSTOM_PROMPT }

            ModeSelectChip(
                label = "FREIGHT",
                isSelected = selectedMode == ProfileMode.CARRIER_DISPATCH,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedMode = ProfileMode.CARRIER_DISPATCH }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Content
        when (selectedMode) {
            ProfileMode.GENERAL_CONVERSATIONAL -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF101412))
                        .border(1.dp, Color(0xFF1E2822), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "GLOBAL FREE / OPEN CONVERSATION",
                            color = palette.statusActive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Unconstrained intelligent voice assistant. Delivers concise, natural answers to any query without domain or profile assumptions.",
                            color = palette.textMetadata,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            ProfileMode.CANDIDATE_INTERVIEW -> {
                Text(
                    text = "CANDIDATE RESUME & INTERVIEW CONTEXT",
                    color = palette.textMetadata,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Information entered here is stored strictly on your device in private memory.",
                    color = palette.textMetadata.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                MultiLineInputField(
                    value = candidateContext,
                    placeholder = "Paste your resume summary, key technologies, notable projects, or interview instructions here...",
                    palette = palette,
                    onValueChange = { candidateContext = it }
                )
            }

            ProfileMode.CUSTOM_PROMPT -> {
                Text(
                    text = "CUSTOM SYSTEM PROMPT / ROLE CONTEXT",
                    color = palette.textMetadata,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure specific instructions for the AI assistant (tone, rules, persona).",
                    color = palette.textMetadata.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                MultiLineInputField(
                    value = customPrompt,
                    placeholder = "Enter custom instructions here (e.g. 'Act as a Senior Distributed Systems Architect...')...",
                    palette = palette,
                    onValueChange = { customPrompt = it }
                )
            }

            ProfileMode.CARRIER_DISPATCH -> {
                ProfileInputField("Company Name", companyName, palette) { companyName = it }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("MC Number", mcNumber, palette) { mcNumber = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("USDOT Number", dotNumber, palette) { dotNumber = it }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("Fleet Size (Trucks)", fleetSize, palette, KeyboardType.Number) { fleetSize = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("Home Base City, ST", homeBase, palette) { homeBase = it }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                ProfileInputField("Equipment Specifications", equipment, palette) { equipment = it }

                Spacer(modifier = Modifier.height(10.dp))
                ProfileInputField("Factoring Company & NOA Details", factoring, palette) { factoring = it }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("Standard Detention ($/hr)", detentionRate, palette, KeyboardType.Decimal) { detentionRate = it }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileInputField("Standard TONU Fee ($)", tonuFee, palette, KeyboardType.Decimal) { tonuFee = it }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Teleprompter Font Size Selector
        Text(
            text = "TELEPROMPTER FONT SIZE",
            color = palette.highlightAcronym,
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
            FontSizeChip(
                label = "NORMAL (17sp)",
                isSelected = selectedFontSize == TeleprompterFontSize.NORMAL,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedFontSize = TeleprompterFontSize.NORMAL }

            FontSizeChip(
                label = "LARGE (20sp)",
                isSelected = selectedFontSize == TeleprompterFontSize.LARGE,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedFontSize = TeleprompterFontSize.LARGE }

            FontSizeChip(
                label = "EXTRA (24sp)",
                isSelected = selectedFontSize == TeleprompterFontSize.EXTRA_LARGE,
                palette = palette,
                modifier = Modifier.weight(1f)
            ) { selectedFontSize = TeleprompterFontSize.EXTRA_LARGE }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.statusActive)
                .clickable {
                    val updated = CarrierProfile(
                        profileMode = selectedMode,
                        candidateContext = candidateContext.trim(),
                        customPrompt = customPrompt.trim(),
                        fontSize = selectedFontSize,
                        companyName = companyName.trim(),
                        mcNumber = mcNumber.trim(),
                        dotNumber = dotNumber.trim(),
                        fleetSize = fleetSize.toIntOrNull() ?: currentProfile.fleetSize,
                        equipmentTypes = equipment.trim(),
                        factoringCompany = factoring.trim(),
                        standardDetentionRate = detentionRate.toDoubleOrNull() ?: currentProfile.standardDetentionRate,
                        standardTonuFee = tonuFee.toDoubleOrNull() ?: currentProfile.standardTonuFee,
                        homeBase = homeBase.trim()
                    )
                    onSaveProfile(updated)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SAVE PROFILE",
                color = palette.background,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ModeSelectChip(
    label: String,
    isSelected: Boolean,
    palette: StealthPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) palette.statusActive.copy(alpha = 0.2f) else Color(0xFF121212))
            .border(
                1.dp,
                if (isSelected) palette.statusActive else Color(0xFF262626),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) palette.statusActive else palette.textMetadata,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FontSizeChip(
    label: String,
    isSelected: Boolean,
    palette: StealthPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) palette.highlightAcronym.copy(alpha = 0.2f) else Color(0xFF121212))
            .border(
                1.dp,
                if (isSelected) palette.highlightAcronym else Color(0xFF262626),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) palette.highlightAcronym else palette.textMetadata,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MultiLineInputField(
    value: String,
    placeholder: String,
    palette: StealthPalette,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101010))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = palette.textMetadata.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = palette.textAnswer,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            ),
            cursorBrush = SolidColor(palette.highlightAcronym),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    palette: StealthPalette,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = palette.textMetadata,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF101010))
                .border(1.dp, Color(0xFF242424), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = palette.textAnswer,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(palette.highlightAcronym),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
