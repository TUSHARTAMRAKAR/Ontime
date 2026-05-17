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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlarmScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    var vibrate by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "New Alarm",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background,
            ),
        )

        // Time display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatTime(hour, minute),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                letterSpacing = (-2).sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hour and minute pickers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hour picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HOUR",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                NumberPicker(
                    value = hour,
                    range = 0..23,
                    onValueChange = { hour = it },
                )
            }

            // Minute picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MINUTE",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                NumberPicker(
                    value = minute,
                    range = 0..59,
                    onValueChange = { minute = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Label input
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "LABEL",
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = {
                    Text(
                        text = "Wake up, Gym, Meeting...",
                        color = TextMuted,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Repeat days
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "REPEAT",
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            DaySelector(
                selectedDays = selectedDays,
                onDayToggle = { day ->
                    selectedDays = if (selectedDays.contains(day)) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vibrate toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Vibrate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
                Text(
                    text = "Phone vibrates when alarm fires",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
            Switch(
                checked = vibrate,
                onCheckedChange = { vibrate = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Surface,
                ),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Button(
            onClick = {
                viewModel.createAlarm(
                    hour = hour,
                    minute = minute,
                    label = label.ifEmpty { "Alarm" },
                    repeatDays = selectedDays.sorted().joinToString(","),
                    vibrate = vibrate,
                )
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Create Alarm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Number Picker ────────────────────────────────────────────────────────────
@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Minus button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable {
                    if (value > range.first) onValueChange(value - 1)
                    else onValueChange(range.last)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "−",
                fontSize = 20.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        // Value display
        Text(
            text = value.toString().padStart(2, '0'),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp),
        )

        // Plus button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface)
                .clickable {
                    if (value < range.last) onValueChange(value + 1)
                    else onValueChange(range.first)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─── Day Selector ─────────────────────────────────────────────────────────────
@Composable
fun DaySelector(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit,
) {
    val days = listOf(
        2 to "M",
        3 to "T",
        4 to "W",
        5 to "T",
        6 to "F",
        7 to "S",
        1 to "S",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val isSelected = selectedDays.contains(dayNum)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Primary else Surface)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) Primary else Border,
                        shape = CircleShape,
                    )
                    .clickable { onDayToggle(dayNum) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else TextMuted,
                )
            }
        }
    }
}