package com.tushartamrakar.ontime.alarm.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.alarm.data.location.LocationHelper
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.alarm.domain.emoji
import com.tushartamrakar.ontime.alarm.domain.title
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.navigation.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitAlarmScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // ─── Label ────────────────────────────────────────────────────────────────
    var habitLabel by remember { mutableStateOf("") }
    var isLabelConfirmed by remember { mutableStateOf(false) }

    // ─── Times ────────────────────────────────────────────────────────────────
    var times by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    // ─── Days ─────────────────────────────────────────────────────────────────
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    val allDays = setOf(1, 2, 3, 4, 5, 6, 7)
    val isDaily = selectedDays == allDays

    // ─── Wake-Up Tasks ────────────────────────────────────────────────────────
    var wakeUpTasks by remember { mutableStateOf(listOf<WakeUpTask>()) }
    var showTaskPicker by remember { mutableStateOf(false) }

    // ─── Are You Up ───────────────────────────────────────────────────────────
    var riseCheckMinutes by remember { mutableStateOf<Int?>(null) }

    // ─── Sound ────────────────────────────────────────────────────────────────
    var selectedSound by remember { mutableStateOf("alarm_digital_alarm") }
    var alarmVolume by remember { mutableStateOf(1.0f) }
    var vibrate by remember { mutableStateOf(true) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val currentToneName = remember(selectedSound) {
        selectedSound.replace("alarm_", "").replace("_", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    // ─── Gentle Wake-Up ───────────────────────────────────────────────────────
    var gentleWakeUpSeconds by remember { mutableStateOf(0) }

    // ─── Time Announcement ────────────────────────────────────────────────────
    var timeAnnouncement by remember { mutableStateOf(false) }
    var announcementVoice by remember { mutableStateOf("female") }
    var isSamplePlaying by remember { mutableStateOf(false) }
    var sampleTts by remember { mutableStateOf<TextToSpeech?>(null) }

    // ─── Weather Reminder ─────────────────────────────────────────────────────
    var weatherReminder by remember { mutableStateOf(false) }
    var isWeatherSamplePlaying by remember { mutableStateOf(false) }
    var weatherSampleTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var locationStatus by remember { mutableStateOf("idle") }

    // ─── Label Reminder ───────────────────────────────────────────────────────
    var labelReminder by remember { mutableStateOf(false) }

    // ─── Extra Loud ───────────────────────────────────────────────────────────
    var extraLoud by remember { mutableStateOf(false) }

    // ─── Snooze ───────────────────────────────────────────────────────────────
    var snoozeEnabled by remember { mutableStateOf(true) }
    var snoozeIntervalMinutes by remember { mutableStateOf(5) }
    var snoozeLimit by remember { mutableStateOf(3) }
    var snoozeProgressiveMode by remember { mutableStateOf(false) }

    // ─── Time Picker State ────────────────────────────────────────────────────
    var showTimePicker by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var pickerHour by remember { mutableStateOf(6) }
    var pickerMinute by remember { mutableStateOf(0) }
    var pickerIsAm by remember { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ─── Check saved location ─────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val saved = LocationHelper.getSavedLocation(context)
        if (saved != null) locationStatus = "saved"
    }

    // ─── Location fetch helper ────────────────────────────────────────────────
    fun fetchLocation() {
        locationStatus = "fetching"
        LocationHelper.fetchAndSaveLocation(
            context = context,
            onSuccess = { Handler(Looper.getMainLooper()).post { locationStatus = "saved" } },
            onFailure = { Handler(Looper.getMainLooper()).post { locationStatus = "failed" } },
        )
    }

    // ─── Location permission launcher ────────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation() else locationStatus = "permission_needed"
    }

    // ─── Receive navigation results ───────────────────────────────────────────
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<String?>("selected_sound", null)?.collect { sound ->
            if (sound != null) { selectedSound = sound; handle.remove<String>("selected_sound") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Int?>("gentle_wake_up_seconds", null)?.collect { seconds ->
            if (seconds != null) { gentleWakeUpSeconds = seconds; handle.remove<Int>("gentle_wake_up_seconds") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Boolean?>("extra_loud_enabled", null)?.collect { enabled ->
            if (enabled == true) { extraLoud = true; handle.remove<Boolean>("extra_loud_enabled") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Boolean?>("snooze_enabled", null)?.collect { v ->
            if (v != null) { snoozeEnabled = v; handle.remove<Boolean>("snooze_enabled") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Int?>("snooze_interval", null)?.collect { v ->
            if (v != null) { snoozeIntervalMinutes = v; handle.remove<Int>("snooze_interval") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Int?>("snooze_limit", null)?.collect { v ->
            if (v != null) { snoozeLimit = v; handle.remove<Int>("snooze_limit") }
        }
    }
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Boolean?>("snooze_progressive", null)?.collect { v ->
            if (v != null) { snoozeProgressiveMode = v; handle.remove<Boolean>("snooze_progressive") }
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.apply { if (isPlaying) stop(); release() }
            previewPlayer = null
            sampleTts?.apply { stop(); shutdown() }
            sampleTts = null
            weatherSampleTts?.apply { stop(); shutdown() }
            weatherSampleTts = null
        }
    }

    // ─── Suggestions ──────────────────────────────────────────────────────────
    val suggestedHabits = listOf(
        "🐣" to "Wake Up Early",
        "💧" to "Drink Water",
        "💪" to "5 min Exercise",
        "📖" to "Read",
        "🧘" to "1-min Meditation",
    )

    // ─── Time helpers ─────────────────────────────────────────────────────────
    fun to24Hour(h12: Int, isAm: Boolean): Int = when {
        isAm && h12 == 12 -> 0
        !isAm && h12 != 12 -> h12 + 12
        else -> h12
    }

    fun to12Hour(h24: Int): Pair<Int, Boolean> {
        val isAm = h24 < 12
        val h = when { h24 == 0 -> 12; h24 > 12 -> h24 - 12; else -> h24 }
        return h to isAm
    }

    fun formatDisplay(h24: Int, min: Int): String {
        val (h, isAm) = to12Hour(h24)
        return "$h:${min.toString().padStart(2, '0')} ${if (isAm) "am" else "pm"}"
    }

    fun openPickerForNew() {
        editingIndex = null; pickerHour = 6; pickerMinute = 0; pickerIsAm = true
        showTimePicker = true
    }

    fun openPickerForEdit(index: Int) {
        editingIndex = index
        val (h24, min) = times[index]
        val (h12, isAm) = to12Hour(h24)
        pickerHour = h12; pickerMinute = min; pickerIsAm = isAm
        showTimePicker = true
    }

    fun saveTime() {
        val h24 = to24Hour(pickerHour, pickerIsAm)
        val idx = editingIndex
        times = if (idx != null) {
            times.toMutableList().also { it[idx] = h24 to pickerMinute }
        } else { times + (h24 to pickerMinute) }
        showTimePicker = false
    }

    val repeatDaysString = if (selectedDays.isEmpty()) "" else selectedDays.sorted().joinToString(",")

    // ─── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier.fillMaxSize().background(Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { focusManager.clearFocus(); navController.navigateUp() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Habit alarm", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextPrimary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // ─── Scrollable Content ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Label Section ─────────────────────────────────────────────
                if (isLabelConfirmed) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(CardBackground).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = "✏️", fontSize = 18.sp)
                        Text(text = habitLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = MulishFamily, color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { isLabelConfirmed = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(CardBackground).padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(text = "✏️", fontSize = 20.sp)
                            BasicTextField(
                                value = habitLabel, onValueChange = { habitLabel = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp,
                                    fontFamily = MulishFamily, fontWeight = FontWeight.SemiBold),
                                cursorBrush = SolidColor(Primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (habitLabel.isNotBlank()) isLabelConfirmed = true
                                }),
                                decorationBox = { innerTextField ->
                                    if (habitLabel.isEmpty()) {
                                        Text(text = "Enter your habit goal", color = TextMuted,
                                            fontSize = 15.sp, fontFamily = MulishFamily)
                                    }
                                    innerTextField()
                                },
                            )
                            if (habitLabel.isNotEmpty()) {
                                IconButton(onClick = { habitLabel = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (habitLabel.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Primary).clickable { focusManager.clearFocus(); isLabelConfirmed = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "Set Habit →", color = TextPrimary, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Recommended", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily, color = TextMuted, letterSpacing = 0.3.sp,
                        modifier = Modifier.padding(horizontal = 4.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    suggestedHabits.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { (emoji, name) ->
                                Row(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                        .background(CardBackground)
                                        .clickable { focusManager.clearFocus(); habitLabel = name; isLabelConfirmed = true }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                    Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = TextPrimary)
                                }
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // ── Sections below label ───────────────────────────────────────
                AnimatedVisibility(
                    visible = isLabelConfirmed,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(28.dp))

                        // ── Alarm Time ────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = "Alarm time", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = MulishFamily, color = TextMuted, letterSpacing = 0.3.sp)
                            Text(text = "${times.size}/5", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily, color = if (times.size >= 5) Primary else TextMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)) {
                            times.forEachIndexed { index, (h24, min) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { openPickerForEdit(index) }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(text = formatDisplay(h24, min), fontSize = 30.sp, fontWeight = FontWeight.Black,
                                        fontFamily = MulishFamily, color = TextPrimary, letterSpacing = (-0.5).sp)
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                            .background(Color(0xFFB00020).copy(alpha = 0.12f))
                                            .clickable { times = times.toMutableList().also { it.removeAt(index) } },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Remove time",
                                            tint = Color(0xFFB00020), modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < times.size - 1) {
                                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                                }
                            }
                            if (times.size < 5) {
                                if (times.isNotEmpty()) Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { openPickerForNew() }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Add time", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = Primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // ── Repeat Days ───────────────────────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = getSmartRepeatLabel(selectedDays), fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                    color = if (selectedDays.isEmpty()) TextMuted else Primary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { selectedDays = if (isDaily) emptySet() else allDays },
                                ) {
                                    Icon(
                                        imageVector = if (isDaily) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = "Daily",
                                        tint = if (isDaily) Primary else TextMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(text = "Daily", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = if (isDaily) Primary else TextMuted)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            val days = listOf(2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S", 1 to "S")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                days.forEach { (dayNum, dayLabel) ->
                                    val isSelected = selectedDays.contains(dayNum)
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(if (isSelected) Primary else SurfaceHigh)
                                            .border(width = 1.5.dp, color = if (isSelected) Primary else Border, shape = CircleShape)
                                            .clickable { selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text = dayLabel, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = if (isSelected) Color.White else TextMuted)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Wake-Up Tasks ─────────────────────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(text = "Wake-Up Tasks", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                        fontFamily = MulishFamily, color = TextPrimary)
                                    Text(text = "Complete tasks to dismiss alarm", fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                }
                                Text(text = "${wakeUpTasks.size}/5", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily, color = if (wakeUpTasks.isEmpty()) TextMuted else Primary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (i in 0 until 5) {
                                    val task = wakeUpTasks.getOrNull(i)
                                    Box(
                                        modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(12.dp))
                                            .background(if (task != null) Primary.copy(alpha = 0.15f) else SurfaceHigh)
                                            .border(width = 1.5.dp, color = if (task != null) Primary else Border, shape = RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (task != null) wakeUpTasks = wakeUpTasks.toMutableList().also { it.removeAt(i) }
                                                else if (wakeUpTasks.size < 5) showTaskPicker = true
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (task != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                Text(text = task.emoji(), fontSize = 22.sp)
                                                Text(text = task.title(), fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                                    fontFamily = MulishFamily, color = Primary, textAlign = TextAlign.Center, maxLines = 1)
                                            }
                                        } else {
                                            Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                                fontFamily = MulishFamily, color = TextMuted)
                                        }
                                    }
                                }
                            }
                            if (wakeUpTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Tap a task to remove it", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                    fontFamily = MulishFamily, color = TextMuted, modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Are You Up? ───────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(14.dp))
                                .clickable {
                                    riseCheckMinutes = when (riseCheckMinutes) {
                                        null -> 5; 5 -> 10; 10 -> 15; else -> null
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "Are You Up?", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                        fontFamily = MulishFamily, color = TextPrimary)
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(Danger.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(text = "HOT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = Danger, letterSpacing = 1.sp)
                                    }
                                }
                                Text(text = "Re-check if you're awake after dismissal", fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                            }
                            Text(
                                text = if (riseCheckMinutes != null) "${riseCheckMinutes}m ›" else "Off ›",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                color = if (riseCheckMinutes != null) Primary else TextMuted,
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Sound + Volume + Vibrate + Gentle Wake-Up + Announcements ──
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            // Play + Name + Arrow
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .background(if (isPreviewPlaying) Primary else SurfaceHigh)
                                        .clickable {
                                            if (isPreviewPlaying) {
                                                previewPlayer?.apply { if (isPlaying) stop(); release() }
                                                previewPlayer = null; isPreviewPlaying = false
                                            } else {
                                                val resId = context.resources.getIdentifier(selectedSound, "raw", context.packageName)
                                                if (resId != 0) {
                                                    previewPlayer = MediaPlayer.create(context, resId)?.apply {
                                                        setOnCompletionListener { isPreviewPlaying = false; release(); previewPlayer = null }
                                                        start()
                                                    }
                                                    isPreviewPlaying = true
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isPreviewPlaying) Color.White else TextMuted,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Text(text = currentToneName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily, color = TextPrimary, modifier = Modifier.weight(1f))
                                IconButton(onClick = { navController.navigate(Screen.AlarmSound.createRoute(selectedSound)) }) {
                                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Choose Sound",
                                        tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Volume
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "🔈", fontSize = 18.sp)
                                Slider(
                                    value = alarmVolume, onValueChange = { alarmVolume = it },
                                    valueRange = 0f..1f, modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary, inactiveTrackColor = SurfaceHigh),
                                )
                                Text(text = "🔊", fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Vibrate
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = "Vibrate", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = TextPrimary)
                                    Text(text = "Phone vibrates when alarm fires", fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                }
                                Switch(checked = vibrate, onCheckedChange = { vibrate = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Primary,
                                        uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Gentle Wake-Up
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceHigh)
                                    .clickable { navController.navigate(Screen.GentleWakeUp.createRoute(gentleWakeUpSeconds)) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(text = "Gentle Wake-Up", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                        fontFamily = MulishFamily, color = TextPrimary)
                                    Text(text = "Gradually increase volume from silence", fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = gentleWakeUpLabel(gentleWakeUpSeconds), fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                        color = if (gentleWakeUpSeconds > 0) Primary else TextMuted)
                                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Time Announcement
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(SurfaceHigh).padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Time Announcement", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = TextPrimary)
                                        Text(text = "Announces greeting + date + time on alarm", fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                    }
                                    Switch(checked = timeAnnouncement, onCheckedChange = { timeAnnouncement = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Primary,
                                            uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh))
                                }
                                if (timeAnnouncement) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        listOf("female" to "Female", "male" to "Male").forEach { (voice, voiceLabel) ->
                                            val isSelected = announcementVoice == voice
                                            Box(
                                                modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Primary else CardBackground)
                                                    .border(width = 1.5.dp, color = if (isSelected) Primary else Border, shape = RoundedCornerShape(12.dp))
                                                    .clickable { announcementVoice = voice },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(text = voiceLabel, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = MulishFamily, color = if (isSelected) Color.White else TextMuted)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.height(44.dp).clip(RoundedCornerShape(12.dp))
                                                .background(if (isSamplePlaying) Danger.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f))
                                                .border(width = 1.5.dp, color = if (isSamplePlaying) Danger else Primary, shape = RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (isSamplePlaying) {
                                                        sampleTts?.stop(); sampleTts?.shutdown(); sampleTts = null; isSamplePlaying = false
                                                    } else {
                                                        isSamplePlaying = true
                                                        var newTts: TextToSpeech? = null
                                                        newTts = TextToSpeech(context) { status ->
                                                            if (status == TextToSpeech.SUCCESS) {
                                                                Handler(Looper.getMainLooper()).postDelayed({
                                                                    when (announcementVoice) {
                                                                        "male" -> { newTts?.setPitch(0.6f); newTts?.setSpeechRate(0.9f) }
                                                                        else -> { newTts?.setPitch(1.8f); newTts?.setSpeechRate(0.9f) }
                                                                    }
                                                                    newTts?.language = Locale.US
                                                                    newTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                                                        override fun onStart(id: String?) {}
                                                                        override fun onDone(id: String?) {
                                                                            Handler(Looper.getMainLooper()).post { isSamplePlaying = false; newTts?.shutdown(); sampleTts = null }
                                                                        }
                                                                        override fun onError(id: String?) {
                                                                            Handler(Looper.getMainLooper()).post { isSamplePlaying = false; newTts?.shutdown(); sampleTts = null }
                                                                        }
                                                                    })
                                                                    val params = Bundle()
                                                                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sample")
                                                                    newTts?.speak(buildSampleAnnouncementText(), TextToSpeech.QUEUE_FLUSH, params, "sample")
                                                                }, 300)
                                                            } else { isSamplePlaying = false }
                                                        }
                                                        sampleTts = newTts
                                                    }
                                                }
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(imageVector = if (isSamplePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Sample", tint = if (isSamplePlaying) Danger else Primary, modifier = Modifier.size(16.dp))
                                                Text(text = if (isSamplePlaying) "Stop" else "Sample", fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                                    color = if (isSamplePlaying) Danger else Primary)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Weather Reminder
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(SurfaceHigh).padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Weather Reminder", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = TextPrimary)
                                        Text(text = "Announces weather conditions on alarm", fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                    }
                                    Switch(
                                        checked = weatherReminder,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                weatherReminder = true
                                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                if (hasFine || hasCoarse) fetchLocation()
                                                else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                            } else { weatherReminder = false; locationStatus = "idle" }
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Primary,
                                            uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh),
                                    )
                                }
                                if (weatherReminder) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .background(when (locationStatus) { "saved" -> Primary.copy(alpha = 0.1f); "failed" -> Danger.copy(alpha = 0.1f); else -> CardBackground })
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        when (locationStatus) {
                                            "fetching" -> {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Primary, strokeWidth = 2.dp)
                                                Text(text = "Fetching your location...", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                    fontFamily = MulishFamily, color = TextMuted)
                                            }
                                            "saved" -> {
                                                Text(text = "📍", fontSize = 14.sp)
                                                Text(text = "Location saved! Weather ready", fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                                            }
                                            "failed" -> {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(text = "❌", fontSize = 14.sp)
                                                        Text(text = "GPS not found. Please turn on Location.", fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Danger)
                                                    }
                                                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                                                        .background(Danger.copy(alpha = 0.1f)).border(1.dp, Danger, RoundedCornerShape(10.dp))
                                                        .clickable { val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS); context.startActivity(intent) },
                                                        contentAlignment = Alignment.Center) {
                                                        Text(text = "Turn On Location Services", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = MulishFamily, color = Danger)
                                                    }
                                                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                                                        .background(Primary.copy(alpha = 0.1f)).border(1.dp, Primary, RoundedCornerShape(10.dp))
                                                        .clickable { fetchLocation() },
                                                        contentAlignment = Alignment.Center) {
                                                        Text(text = "Retry", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                                            fontFamily = MulishFamily, color = Primary)
                                                    }
                                                }
                                            }
                                            "permission_needed" -> {
                                                Text(text = "⚙️", fontSize = 14.sp)
                                                Text(text = "Allow location in Settings → Apps → Ontime → Permissions",
                                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Danger)
                                            }
                                            else -> {
                                                Text(text = "📡", fontSize = 14.sp)
                                                Text(text = "Waiting for location...", fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                            }
                                        }
                                    }
                                    if (locationStatus == "permission_needed") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                                            .background(Danger.copy(alpha = 0.1f)).border(1.dp, Danger, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            }, contentAlignment = Alignment.Center) {
                                            Text(text = "Open App Settings", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                                fontFamily = MulishFamily, color = Danger)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                                            .background(Primary.copy(alpha = 0.1f)).border(1.dp, Primary, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                if (hasFine || hasCoarse) fetchLocation() else locationStatus = "permission_needed"
                                            }, contentAlignment = Alignment.Center) {
                                            Text(text = "Retry After Granting Permission", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                                fontFamily = MulishFamily, color = Primary)
                                        }
                                    }
                                    if (locationStatus == "saved") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                                                .background(if (isWeatherSamplePlaying) Danger.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f))
                                                .border(width = 1.5.dp, color = if (isWeatherSamplePlaying) Danger else Primary, shape = RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (isWeatherSamplePlaying) {
                                                        weatherSampleTts?.stop(); weatherSampleTts?.shutdown(); weatherSampleTts = null; isWeatherSamplePlaying = false
                                                    } else {
                                                        isWeatherSamplePlaying = true
                                                        val sampleWeatherText = "Here is today's weather update. It's a beautiful sunny day outside! Current temperature is 28 degrees Celsius. Today's high is 32 degrees and the low is 22 degrees. Air quality is good today."
                                                        var newTts: TextToSpeech? = null
                                                        newTts = TextToSpeech(context) { status ->
                                                            if (status == TextToSpeech.SUCCESS) {
                                                                Handler(Looper.getMainLooper()).postDelayed({
                                                                    when (announcementVoice) {
                                                                        "male" -> { newTts?.setPitch(0.6f); newTts?.setSpeechRate(0.9f) }
                                                                        else -> { newTts?.setPitch(1.8f); newTts?.setSpeechRate(0.9f) }
                                                                    }
                                                                    newTts?.language = Locale.US
                                                                    newTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                                                        override fun onStart(id: String?) {}
                                                                        override fun onDone(id: String?) { Handler(Looper.getMainLooper()).post { isWeatherSamplePlaying = false; newTts?.shutdown(); weatherSampleTts = null } }
                                                                        override fun onError(id: String?) { Handler(Looper.getMainLooper()).post { isWeatherSamplePlaying = false; newTts?.shutdown(); weatherSampleTts = null } }
                                                                    })
                                                                    val params = Bundle()
                                                                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "weather_sample")
                                                                    newTts?.speak(sampleWeatherText, TextToSpeech.QUEUE_FLUSH, params, "weather_sample")
                                                                }, 300)
                                                            } else { isWeatherSamplePlaying = false }
                                                        }
                                                        weatherSampleTts = newTts
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(imageVector = if (isWeatherSamplePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Weather Sample", tint = if (isWeatherSamplePlaying) Danger else Primary, modifier = Modifier.size(16.dp))
                                                Text(text = if (isWeatherSamplePlaying) "Stop" else "Sample", fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                                    color = if (isWeatherSamplePlaying) Danger else Primary)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Label Reminder
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(SurfaceHigh).padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Label Reminder", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = TextPrimary)
                                        Text(text = "Reads your alarm label out loud", fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
                                    }
                                    Switch(checked = labelReminder, onCheckedChange = { labelReminder = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Primary,
                                            uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh))
                                }
                                if (labelReminder) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val sampleLabelText = habitLabel.ifBlank { "No label set. Please add a label first." }
                                    var isLabelSamplePlaying by remember { mutableStateOf(false) }
                                    var labelSampleTts by remember { mutableStateOf<TextToSpeech?>(null) }
                                    if (habitLabel.isNotBlank()) {
                                        Text(text = "Will say: \"$habitLabel\"", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                            fontFamily = MulishFamily, color = Primary, modifier = Modifier.padding(bottom = 8.dp))
                                    } else {
                                        Text(text = "Add a label above to use this feature", fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted,
                                            modifier = Modifier.padding(bottom = 8.dp))
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                                            .background(if (isLabelSamplePlaying) Danger.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f))
                                            .border(width = 1.5.dp, color = if (isLabelSamplePlaying) Danger else Primary, shape = RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (isLabelSamplePlaying) {
                                                    labelSampleTts?.stop(); labelSampleTts?.shutdown(); labelSampleTts = null; isLabelSamplePlaying = false
                                                } else {
                                                    isLabelSamplePlaying = true
                                                    var newTts: TextToSpeech? = null
                                                    newTts = TextToSpeech(context) { status ->
                                                        if (status == TextToSpeech.SUCCESS) {
                                                            Handler(Looper.getMainLooper()).postDelayed({
                                                                when (announcementVoice) {
                                                                    "male" -> { newTts?.setPitch(0.6f); newTts?.setSpeechRate(0.9f) }
                                                                    else -> { newTts?.setPitch(1.8f); newTts?.setSpeechRate(0.9f) }
                                                                }
                                                                newTts?.language = Locale.US
                                                                newTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                                                    override fun onStart(id: String?) {}
                                                                    override fun onDone(id: String?) { Handler(Looper.getMainLooper()).post { isLabelSamplePlaying = false; newTts?.shutdown(); labelSampleTts = null } }
                                                                    override fun onError(id: String?) { Handler(Looper.getMainLooper()).post { isLabelSamplePlaying = false; newTts?.shutdown(); labelSampleTts = null } }
                                                                })
                                                                val params = Bundle()
                                                                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label_sample")
                                                                newTts?.speak(sampleLabelText, TextToSpeech.QUEUE_FLUSH, params, "label_sample")
                                                            }, 300)
                                                        } else { isLabelSamplePlaying = false }
                                                    }
                                                    labelSampleTts = newTts
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = if (isLabelSamplePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "Label Sample", tint = if (isLabelSamplePlaying) Danger else Primary, modifier = Modifier.size(16.dp))
                                            Text(text = if (isLabelSamplePlaying) "Stop" else "Sample", fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                                color = if (isLabelSamplePlaying) Danger else Primary)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Extra Loud Effect ─────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (extraLoud) Danger.copy(alpha = 0.08f) else SurfaceHigh)
                                .border(width = if (extraLoud) 1.5.dp else 0.dp,
                                    color = if (extraLoud) Danger.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = if (extraLoud) "💥 Extra Loud Effect" else "🔊 Extra Loud Effect",
                                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                        color = if (extraLoud) Danger else TextPrimary)
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape).background(CardBackground)
                                            .border(1.dp, Border, CircleShape)
                                            .clickable { navController.navigate(Screen.ExtraLoudInfo.createRoute(selectedSound)) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text = "?", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily, color = TextMuted)
                                    }
                                }
                                Text(
                                    text = if (extraLoud) "Blasts aggressive sounds after 35 sec!" else "For heavy sleepers — blasts at MAX volume",
                                    fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily,
                                    color = if (extraLoud) Danger.copy(alpha = 0.8f) else TextMuted,
                                )
                            }
                            Switch(checked = extraLoud, onCheckedChange = { extraLoud = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Danger,
                                    uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Custom Settings label ─────────────────────────────
                        Text(text = "CUSTOM SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = TextMuted, letterSpacing = 1.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Snooze Row ────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(CardBackground).border(1.dp, Border, RoundedCornerShape(16.dp))
                                .clickable {
                                    navController.navigate(Screen.SnoozeSettings.createRoute(snoozeEnabled, snoozeIntervalMinutes, snoozeLimit, snoozeProgressiveMode))
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(text = "💤 Snooze", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily, color = TextPrimary)
                                Text(text = buildSnoozeSummary(snoozeEnabled, snoozeIntervalMinutes, snoozeLimit, snoozeProgressiveMode),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                    color = if (snoozeEnabled) Primary else TextMuted)
                            }
                            Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Snooze Settings",
                                tint = TextMuted, modifier = Modifier.size(14.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // ─── Create Button ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isLabelConfirmed && times.isNotEmpty(),
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
            ) {
                Button(
                    onClick = {
                        times.forEach { (h24, min) ->
                            viewModel.createAlarm(
                                hour = h24, minute = min,
                                label = habitLabel,
                                repeatDays = repeatDaysString,
                                vibrate = vibrate,
                                tasks = wakeUpTasks,
                                riseCheckMinutes = riseCheckMinutes ?: 0,
                                sound = selectedSound,
                                volume = alarmVolume,
                                gentleWakeUpSeconds = gentleWakeUpSeconds,
                                timeAnnouncement = timeAnnouncement,
                                announcementVoice = announcementVoice,
                                weatherReminder = weatherReminder,
                                labelReminder = labelReminder,
                                extraLoud = extraLoud,
                                snoozeEnabled = snoozeEnabled,
                                snoozeIntervalMinutes = snoozeIntervalMinutes,
                                snoozeLimit = snoozeLimit,
                                snoozeProgressiveMode = snoozeProgressiveMode,
                            )
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(text = "Create Habit Alarm", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily, color = Color.White, letterSpacing = 0.3.sp)
                }
            }
        }

        // ─── Task Picker Sheet ────────────────────────────────────────────────
        if (showTaskPicker) {
            WakeUpTaskPickerSheet(
                onTaskSelected = { task -> wakeUpTasks = wakeUpTasks.toMutableList().also { it.add(task) }; showTaskPicker = false },
                onDismiss = { showTaskPicker = false },
            )
        }

        // ─── Time Picker Bottom Sheet ─────────────────────────────────────────
        if (showTimePicker) {
            ModalBottomSheet(
                onDismissRequest = { showTimePicker = false },
                sheetState = bottomSheetState,
                containerColor = CardBackground,
                tonalElevation = 0.dp,
            ) {
                TimePickerSheetContent(
                    hour = pickerHour, minute = pickerMinute, isAm = pickerIsAm,
                    onHourChange = { pickerHour = it }, onMinuteChange = { pickerMinute = it },
                    onAmPmChange = { pickerIsAm = it }, onSave = { saveTime() },
                )
            }
        }
    }
}

// ─── Time Picker Sheet ─────────────────────────────────────────────────────────
@Composable
private fun TimePickerSheetContent(
    hour: Int, minute: Int, isAm: Boolean,
    onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit, onSave: () -> Unit,
) {
    val hours = (1..12).map { it.toString() }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }
    val amPmList = listOf("a.m.", "p.m.")
    val hourIndex = (hour - 1).coerceIn(0, 11)
    val minuteIndex = minute.coerceIn(0, 59)
    val amPmIndex = if (isAm) 0 else 1

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(162.dp)) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(54.dp)
                .background(SurfaceHigh, RoundedCornerShape(10.dp)))
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                WheelColumn(items = hours, selectedIndex = hourIndex, onSelectedChange = { onHourChange(it + 1) }, modifier = Modifier.width(72.dp))
                Text(text = ":", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = MulishFamily,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                WheelColumn(items = minutes, selectedIndex = minuteIndex, onSelectedChange = { onMinuteChange(it) }, modifier = Modifier.width(72.dp))
                Spacer(modifier = Modifier.width(20.dp))
                WheelColumn(items = amPmList, selectedIndex = amPmIndex, onSelectedChange = { onAmPmChange(it == 0) }, modifier = Modifier.width(68.dp))
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Primary).clickable { onSave() }.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Save", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = TextPrimary, letterSpacing = 0.3.sp)
        }
    }
}

// ─── Wheel Picker Column ───────────────────────────────────────────────────────
@Composable
private fun WheelColumn(items: List<String>, selectedIndex: Int, onSelectedChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val itemHeightDp = 54.dp
    val allItems = listOf("") + items + listOf("")
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val realIndex = (centerIndex - 1).coerceIn(0, items.size - 1)
            if (realIndex != selectedIndex) onSelectedChange(realIndex)
        }
    }
    LaunchedEffect(selectedIndex) { if (!listState.isScrollInProgress) listState.scrollToItem(selectedIndex) }

    LazyColumn(state = listState, flingBehavior = snapBehavior, modifier = modifier.height(itemHeightDp * 3)) {
        itemsIndexed(allItems) { paddedIdx, item ->
            val isCenter = paddedIdx == centerIndex
            Box(modifier = Modifier.height(itemHeightDp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (item.isNotEmpty()) {
                    Text(text = item,
                        fontSize = if (isCenter) 30.sp else 20.sp,
                        fontWeight = if (isCenter) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isCenter) TextPrimary else TextMuted,
                        fontFamily = MulishFamily)
                }
            }
        }
    }
}
