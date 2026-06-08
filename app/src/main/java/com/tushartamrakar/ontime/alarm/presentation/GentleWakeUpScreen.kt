package com.tushartamrakar.ontime.alarm.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

data class GentleWakeUpOption(
    val seconds: Int,
    val label: String,
)

val gentleWakeUpOptions = listOf(
    GentleWakeUpOption(0, "Off"),
    GentleWakeUpOption(15, "15 seconds"),
    GentleWakeUpOption(30, "30 seconds"),
    GentleWakeUpOption(60, "1 minute"),
    GentleWakeUpOption(300, "5 minutes"),
    GentleWakeUpOption(600, "10 minutes"),
)

fun gentleWakeUpLabel(seconds: Int): String {
    return gentleWakeUpOptions.find { it.seconds == seconds }?.label ?: "Off"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GentleWakeUpScreen(
    navController: NavHostController,
    currentSeconds: Int,
    onOptionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "Gentle Wake-Up",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // ─── Description ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "🌅 What is Gentle Wake-Up?",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Volume gradually increases from silence to full level over the selected time. Eases you out of deep sleep naturally.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Options ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            gentleWakeUpOptions.forEach { option ->
                val isSelected = currentSeconds == option.seconds

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Primary.copy(alpha = 0.1f) else CardBackground)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Primary else Border,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable {
                            onOptionSelected(option.seconds)
                            navController.popBackStack()
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = option.label,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = if (isSelected) Primary else TextPrimary,
                    )

                    // Radio button
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Primary else TextMuted,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Primary),
                            )
                        }
                    }
                }
            }
        }
    }
}