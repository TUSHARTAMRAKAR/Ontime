package com.tushartamrakar.ontime.alarm.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.navigation.Screen

@Composable
fun AlarmsScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
) {
    val alarms by viewModel.alarms.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "⏰ Alarms",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 24.dp,
                    bottom = 16.dp,
                ),
            )

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "⏰", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No alarms yet",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first alarm",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarms) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                        )
                    }
                }
            }
        }

        // FAB above bottom nav bar
        FloatingActionButton(
            onClick = { navController.navigate(Screen.CreateAlarm.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 24.dp,
                    bottom = bottomPadding + 16.dp,
                ),
            containerColor = Primary,
            contentColor = TextPrimary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Alarm",
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// ─── Alarm Card ───────────────────────────────────────────────────────────────
@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val timeString = formatTime(alarm.hour, alarm.minute)
    val repeatLabel = getRepeatLabel(alarm.repeatDays)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeString,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) TextPrimary else TextMuted,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = alarm.label.ifEmpty { "Alarm" },
                    fontSize = 14.sp,
                    color = if (alarm.isEnabled) TextSecondary else TextMuted,
                )
                Text(
                    text = repeatLabel,
                    fontSize = 12.sp,
                    color = if (alarm.isEnabled) Primary else TextMuted,
                    fontWeight = FontWeight.Medium,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Surface,
                    ),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─── Helper Functions ─────────────────────────────────────────────────────────
fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val displayMinute = minute.toString().padStart(2, '0')
    return "$displayHour:$displayMinute $period"
}

fun getRepeatLabel(repeatDays: String): String {
    if (repeatDays.isBlank()) return "Once"
    val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    return when {
        days.size == 7 -> "Every day"
        days == listOf(2, 3, 4, 5, 6) -> "Weekdays"
        days == listOf(1, 7) -> "Weekends"
        else -> days.joinToString(", ") {
            when (it) {
                1 -> "Sun"
                2 -> "Mon"
                3 -> "Tue"
                4 -> "Wed"
                5 -> "Thu"
                6 -> "Fri"
                7 -> "Sat"
                else -> ""
            }
        }
    }
}