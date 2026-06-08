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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSettingsScreen(
    navController: NavHostController,
    initialEnabled: Boolean,
    initialInterval: Int,
    initialLimit: Int,
    initialProgressive: Boolean,
) {
    var snoozeEnabled by remember { mutableStateOf(initialEnabled) }
    var selectedInterval by remember { mutableStateOf(initialInterval) }
    var selectedLimit by remember { mutableStateOf(initialLimit) }
    var progressiveMode by remember { mutableStateOf(initialProgressive) }

    // ─── Interval options ─────────────────────────────────────────────────────
    val intervalOptions = listOf(1, 5, 10, 15, 20, 30)

    // ─── Limit options: 0 = unlimited ─────────────────────────────────────────
    val limitOptions = listOf(
        0 to "Unlimited",
        1 to "1 time",
        2 to "2 times",
        3 to "3 times",
        5 to "5 times",
    )

    // ─── Smart summary ────────────────────────────────────────────────────────
    val smartSummary = when {
        !snoozeEnabled -> "Snooze is disabled"
        selectedLimit == 0 -> "Unlimited snoozes of $selectedInterval min each"
        progressiveMode -> "Progressive: starts at ${selectedInterval}min, gets shorter each time"
        else -> {
            val totalMins = selectedInterval * selectedLimit
            val hours = totalMins / 60
            val mins = totalMins % 60
            val totalStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            "$selectedInterval min × $selectedLimit = $totalStr max extra sleep"
        }
    }

    // ─── Warning for extreme settings ─────────────────────────────────────────
    val showWarning = snoozeEnabled && selectedLimit == 0 && selectedInterval >= 20

    // ─── Save and go back ─────────────────────────────────────────────────────
    fun saveAndBack() {
        navController.previousBackStackEntry?.savedStateHandle?.apply {
            set("snooze_enabled", snoozeEnabled)
            set("snooze_interval", selectedInterval)
            set("snooze_limit", selectedLimit)
            set("snooze_progressive", progressiveMode)
        }
        navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "Snooze Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = { saveAndBack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // ─── Smart Summary Card ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary.copy(alpha = 0.1f))
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "💤 Snooze Summary",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = smartSummary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = TextPrimary,
            )
        }

        // ─── Warning ──────────────────────────────────────────────────────────
        if (showWarning) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Danger.copy(alpha = 0.1f))
                    .border(1.dp, Danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Text(
                    text = "Unlimited + ${selectedInterval}min could let you sleep for hours!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = Danger,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Snooze ON/OFF ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Snooze",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                Text(
                    text = if (snoozeEnabled) "Tap snooze to delay alarm" else "Snooze button hidden",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                )
            }
            Switch(
                checked = snoozeEnabled,
                onCheckedChange = { snoozeEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceHigh,
                ),
            )
        }

        if (snoozeEnabled) {
            Spacer(modifier = Modifier.height(20.dp))

            // ─── Visual Timeline ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "Snooze Timeline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))

                val visibleCount = if (selectedLimit == 0) 3 else minOf(selectedLimit, 4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    // Alarm start
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = "🔔", fontSize = 16.sp) }
                        Text(
                            text = "Alarm",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = Primary,
                        )
                    }

                    // Snooze steps
                    for (i in 1..visibleCount) {
                        val intervalForStep = if (progressiveMode && i > 1) {
                            maxOf(1, selectedInterval - (i - 1) * 2)
                        } else selectedInterval

                        // Connector line
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .weight(1f)
                                    .background(Primary.copy(alpha = 0.3f))
                            )
                            Text(
                                text = "+${intervalForStep}m",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily,
                                color = TextMuted,
                            )
                        }

                        // Snooze bubble
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceHigh)
                                    .border(1.5.dp, Primary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "💤",
                                    fontSize = 16.sp,
                                )
                            }
                            Text(
                                text = "$i",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily,
                                color = TextMuted,
                            )
                        }
                    }

                    // End indicator
                    if (selectedLimit > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .weight(1f)
                                    .background(Danger.copy(alpha = 0.3f))
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Danger.copy(alpha = 0.15f))
                                    .border(1.5.dp, Danger.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "❌", fontSize = 14.sp)
                            }
                            Text(
                                text = "Done",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily,
                                color = Danger,
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .weight(1f)
                                    .background(Primary.copy(alpha = 0.3f))
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "∞", fontSize = 18.sp, color = Primary, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "No limit",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily,
                                color = Primary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Interval Picker ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Interval",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                    )
                    Text(
                        text = "$selectedInterval minutes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = Primary,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                intervalOptions.forEach { option ->
                    val isSelected = selectedInterval == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedInterval = option }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (option == 1) "1 minute" else "$option minutes",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Primary else TextPrimary,
                        )
                        // Radio button
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) Primary else TextMuted, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Snooze Limit Picker ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Snooze Limit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (selectedLimit == 0) "Unlimited" else "$selectedLimit times",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = Primary,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                limitOptions.forEach { (value, label) ->
                    val isSelected = selectedLimit == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedLimit = value }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = if (value == 0) "∞" else "$value",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = if (isSelected) Primary else TextMuted,
                            )
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color = if (isSelected) Primary else TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) Primary else TextMuted, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Progressive Mode ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (progressiveMode) Primary.copy(alpha = 0.08f) else CardBackground
                    )
                    .border(
                        width = if (progressiveMode) 1.5.dp else 1.dp,
                        color = if (progressiveMode) Primary else Border,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Progressive Mode",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = TextPrimary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "SMART",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = Primary,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Snooze interval gets shorter each time — forces you to wake up!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                        if (progressiveMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val steps = if (selectedLimit == 0) 3 else minOf(selectedLimit, 4)
                            val sequence = (0 until steps).map { i ->
                                maxOf(1, selectedInterval - i * 2)
                            }
                            Text(
                                text = "Pattern: ${sequence.joinToString(" → ")} min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = Primary,
                            )
                        }
                    }
                    Switch(
                        checked = progressiveMode,
                        onCheckedChange = { progressiveMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Helper to build snooze summary for CreateAlarmScreen ────────────────────
fun buildSnoozeSummary(
    enabled: Boolean,
    intervalMinutes: Int,
    limit: Int,
    progressive: Boolean,
): String {
    if (!enabled) return "Off"
    val limitStr = if (limit == 0) "∞" else "$limit×"
    val progressStr = if (progressive) " (↓)" else ""
    return "$intervalMinutes min, $limitStr$progressStr"
}