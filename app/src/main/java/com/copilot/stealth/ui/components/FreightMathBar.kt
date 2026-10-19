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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.engine.calculator.FreightCalculatorEngine
import com.copilot.stealth.ui.theme.StealthPalette

enum class CalculatorTab {
    DETENTION,
    RPM,
    HOS
}

@Composable
fun FreightMathBar(
    uiState: StealthUiState,
    palette: StealthPalette,
    onQuoteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CalculatorTab.DETENTION) }

    // Detention inputs
    var apptTime by remember { mutableStateOf("08:00") }
    var inTime by remember { mutableStateOf("08:00") }
    var outTime by remember { mutableStateOf("13:30") }
    var detentionRate by remember { mutableStateOf("50") }

    // RPM inputs
    var grossRate by remember { mutableStateOf("2800") }
    var loadedMiles by remember { mutableStateOf("1100") }
    var deadheadMiles by remember { mutableStateOf("100") }

    // HOS inputs
    var driveHoursRemaining by remember { mutableStateOf("8.5") }
    var tripMiles by remember { mutableStateOf("450") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        // Tab row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalculatorTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFF222222) else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tab.name,
                            color = if (isSelected) palette.textAnswer else palette.textMetadata,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Text(
                text = "MATH ENGINE",
                color = palette.textMetadata,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            CalculatorTab.DETENTION -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniInputField("Appt", apptTime, palette) { apptTime = it }
                    MiniInputField("In", inTime, palette) { inTime = it }
                    MiniInputField("Out", outTime, palette) { outTime = it }
                    MiniInputField("$/h", detentionRate, palette) { detentionRate = it }
                }

                val rateDbl = detentionRate.toDoubleOrNull() ?: 50.0
                val result = remember(apptTime, inTime, outTime, rateDbl) {
                    FreightCalculatorEngine.calculateDetention(apptTime, inTime, outTime, rateDbl, 2.0)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.formattedQuote,
                        color = if (result.billableHours > 0.0) palette.highlightMoney else palette.textAnswer,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    if (result.billableHours > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1B2B1B))
                                .clickable { onQuoteSelected(result.formattedQuote) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("QUOTE", color = palette.highlightMoney, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            CalculatorTab.RPM -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniInputField("Rate$", grossRate, palette) { grossRate = it }
                    MiniInputField("Loaded", loadedMiles, palette) { loadedMiles = it }
                    MiniInputField("Deadhead", deadheadMiles, palette) { deadheadMiles = it }
                }

                val rate = grossRate.toDoubleOrNull() ?: 0.0
                val loaded = loadedMiles.toDoubleOrNull() ?: 1.0
                val dh = deadheadMiles.toDoubleOrNull() ?: 0.0
                val result = remember(rate, loaded, dh) {
                    FreightCalculatorEngine.calculateRpm(rate, loaded, dh)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.formattedSummary,
                        color = palette.highlightAcronym,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF162530))
                            .clickable { onQuoteSelected(result.formattedSummary) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("QUOTE", color = palette.highlightAcronym, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            CalculatorTab.HOS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniInputField("Drive Hours", driveHoursRemaining, palette) { driveHoursRemaining = it }
                    MiniInputField("Trip Miles", tripMiles, palette) { tripMiles = it }
                }

                val hours = driveHoursRemaining.toDoubleOrNull() ?: 0.0
                val miles = tripMiles.toDoubleOrNull() ?: 0.0
                val result = remember(hours, miles) {
                    FreightCalculatorEngine.calculateHosReach(hours, miles)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.formattedSummary,
                    color = if (result.isLegal) palette.highlightTime else Color(0xFFEF5350),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MiniInputField(
    label: String,
    value: String,
    palette: StealthPalette,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(text = label, color = palette.textMetadata, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .width(65.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = palette.textAnswer,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(palette.highlightAcronym),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}
