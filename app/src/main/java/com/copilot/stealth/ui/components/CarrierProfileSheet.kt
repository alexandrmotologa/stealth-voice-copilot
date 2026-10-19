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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.copilot.stealth.ui.theme.StealthPalette

@Composable
fun CarrierProfileSheet(
    currentProfile: CarrierProfile,
    palette: StealthPalette,
    onSaveProfile: (CarrierProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    text = "CARRIER IDENTITY GROUNDING",
                    color = palette.textAnswer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Injected dynamically into interview dialogue responses",
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

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.statusActive)
                .clickable {
                    val updated = CarrierProfile(
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
                text = "SAVE CARRIER PROFILE",
                color = palette.background,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
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
