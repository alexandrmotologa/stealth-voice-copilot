package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.ui.theme.TextMetadata

@Composable
fun QuickScenarioSelector(
    onScenarioSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPrompts = listOf(
        "Detention" to "What is your standard policy when a driver is delayed at a facility past two hours?",
        "TONU" to "The broker cancels the load after the driver arrives on site. What do you do?",
        "Rate Con" to "What do you verify on a rate confirmation before signing?",
        "Layover" to "What happens if a facility cannot unload until the next morning?",
        "HOS & ELD" to "Explain property-carrying Hours of Service regulations to me.",
        "OS&D BOL" to "What should the driver do if the cargo count does not match the BOL?",
        "Lumper" to "How do you handle a $300 lumper fee request at a grocery warehouse?",
        "Weight" to "Your driver scales the load and the drive axle is 35,500 pounds. How do you resolve it?"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPrompts.forEach { (label, prompt) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101010))
                    .clickable { onScenarioSelected(prompt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    color = TextMetadata,
                    fontSize = 11.sp
                )
            }
        }
    }
}
