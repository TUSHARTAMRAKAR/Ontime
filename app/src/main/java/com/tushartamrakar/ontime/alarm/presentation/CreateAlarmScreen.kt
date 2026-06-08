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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.alarm.data.location.LocationHelper
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.alarm.domain.emoji
import com.tushartamrakar.ontime.alarm.domain.title
import com.tushartamrakar.ontime.alarm.domain.toJsonString
import com.tushartamrakar.ontime.alarm.domain.toWakeUpTasks
import com.tushartamrakar.ontime.core.ui.components.WheelPicker
import com.tushartamrakar.ontime.core.ui.components.WheelPickerController
import com.tushartamrakar.ontime.core.ui.components.rememberWheelPickerController
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
import java.util.Calendar
import java.util.Locale

fun getSmartRepeatLabel(selectedDays: Set<Int>): String {
    if (selectedDays.isEmpty()) return "One time"
    val allDays = setOf(1, 2, 3, 4, 5, 6, 7)
    val weekdays = setOf(2, 3, 4, 5, 6)
    val weekends = setOf(1, 6, 7)
    val dayNames = mapOf(
        1 to "Sunday", 2 to "Monday", 3 to "Tuesday",
        4 to "Wednesday", 5 to "Thursday", 6 to "Friday", 7 to "Saturday",
    )
    return when (selectedDays) {
        allDays -> "Daily"
        weekdays -> "Weekdays"
        weekends -> "Weekends"
        else -> if (selectedDays.size == 1) {
            "Only ${dayNames[selectedDays.first()]}s"
        } else {
            selectedDays.sorted().joinToString(", ") { dayNames[it]?.take(3) ?: "" }
        }
    }
}

fun buildSampleAnnouncementText(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val hour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = calendar.get(Calendar.MINUTE)
    val amPm = if (hour < 12) "A M" else "P M"

    val greeting = when (hour) {
        in 5..11 -> "Good Morning!"
        in 12..16 -> "Good Afternoon!"
        in 17..23 -> "Good Evening!"
        else -> "Wake Up!"
    }

    val dayNames = listOf(
        "", "Sunday", "Monday", "Tuesday",
        "Wednesday", "Thursday", "Friday", "Saturday",
    )
    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    val dayOfWeek = dayNames[calendar.get(Calendar.DAY_OF_WEEK)]
    val month = monthNames[calendar.get(Calendar.MONTH) + 1]
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val minuteText = if (minute == 0) "o'clock" else "$minute minutes"

    return "$greeting Today is $dayOfWeek, $month $dayOfMonth. " +
            "The time is $hour12 hours $minuteText $amPm."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlarmScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = hiltViewModel(),
    alarmId: Int = -1,
) {
    val isEditing = alarmId != -1
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var hour12 by rememberSaveable { mutableIntStateOf(7) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var isAm   by rememberSaveable { mutableStateOf(true) }

    // Carry-over: track previous minute to detect 59→00 / 00→59 rollovers
    var prevMinuteIdx by rememberSaveable { mutableIntStateOf(0) }

    // Controllers: allow programmatic scrolling of hour and AM/PM pickers
    val hourController = rememberWheelPickerController()
    val amPmController = rememberWheelPickerController()
    var label      by rememberSaveable { mutableStateOf("") }
    var labelEmoji by rememberSaveable { mutableStateOf("") }  // shown in leadingIcon slot
    var vibrate by rememberSaveable { mutableStateOf(true) }
    var selectedDays by rememberSaveable(
        stateSaver = Saver<Set<Int>, String>(
            save    = { value -> value.sorted().joinToString(",") },
            restore = { s ->
                if (s.isBlank()) setOf()
                else s.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            },
        )
    ) { mutableStateOf(setOf<Int>()) }
    var wakeUpTasks by rememberSaveable(
        stateSaver = Saver<List<WakeUpTask>, String>(
            save    = { value -> value.toJsonString() },
            restore = { s    -> s.toWakeUpTasks() },
        )
    ) { mutableStateOf(listOf<WakeUpTask>()) }
    var showTaskPicker    by remember { mutableStateOf(false) }
    var editingTaskIndex  by remember { mutableStateOf<Int?>(null) }
    var showEmojiPicker   by remember { mutableStateOf(false) }
    var riseCheckMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedSound by rememberSaveable { mutableStateOf("alarm_digital_alarm") }
    var alarmVolume by rememberSaveable { mutableStateOf(1.0f) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var gentleWakeUpSeconds by rememberSaveable { mutableStateOf(0) }
    var timeAnnouncement by rememberSaveable { mutableStateOf(false) }
    var announcementVoice by rememberSaveable { mutableStateOf("female") }
    var isSamplePlaying by remember { mutableStateOf(false) }
    var sampleTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var weatherReminder by rememberSaveable { mutableStateOf(false) }
    var isWeatherSamplePlaying by remember { mutableStateOf(false) }
    var weatherSampleTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var locationStatus by remember { mutableStateOf("idle") }
    var labelReminder by rememberSaveable { mutableStateOf(false) }
    var extraLoud by rememberSaveable { mutableStateOf(false) }
    var snoozeEnabled by rememberSaveable { mutableStateOf(true) }
    var snoozeIntervalMinutes by rememberSaveable { mutableStateOf(5) }
    var snoozeLimit by rememberSaveable { mutableStateOf(3) }
    var snoozeProgressiveMode by rememberSaveable { mutableStateOf(false) }

    // Unsaved changes tracking — declared early so all LaunchedEffects can access them
    var screenReady by rememberSaveable { mutableStateOf(false) }
    var hasChanges  by rememberSaveable { mutableStateOf(false) }

    // ─── Check if location already saved on screen open ───────────────────────
    LaunchedEffect(Unit) {
        val saved = LocationHelper.getSavedLocation(context)
        if (saved != null) locationStatus = "saved"
    }

    // ─── Helper function to fetch location ───────────────────────────────────
    fun fetchLocation() {
        locationStatus = "fetching"
        LocationHelper.fetchAndSaveLocation(
            context = context,
            onSuccess = {
                Handler(Looper.getMainLooper()).post {
                    locationStatus = "saved"
                }
            },
            onFailure = {
                Handler(Looper.getMainLooper()).post {
                    // ✅ Don't turn toggle OFF — just show failed status
                    locationStatus = "failed"
                }
            },
        )
    }

    // ─── Location permission launcher ─────────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // weatherReminder already true → just fetch
            fetchLocation()
        } else {
            // Only turn OFF if user explicitly denied
            // (Realme may silently block — keep ON and show status)
            locationStatus = "permission_needed"
        }
    }

    // ─── Receive selected_sound result ────────────────────────────────────────
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<String?>("selected_sound", null)?.collect { sound ->
            if (sound != null) {
                selectedSound = sound
                handle.remove<String>("selected_sound")
            }
        }
    }

    // ─── Receive gentle_wake_up_seconds result ────────────────────────────────
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Int?>("gentle_wake_up_seconds", null)?.collect { seconds ->
            if (seconds != null) {
                gentleWakeUpSeconds = seconds
                handle.remove<Int>("gentle_wake_up_seconds")
            }
        }
    }

    // ─── Receive extra_loud_enabled from info screen ──────────────────────────
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getStateFlow<Boolean?>("extra_loud_enabled", null)?.collect { enabled ->
            if (enabled == true) {
                extraLoud = true
                handle.remove<Boolean>("extra_loud_enabled")
            }
        }
    }

    // ─── Receive snooze settings results ──────────────────────────────────────
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

    val allDays = setOf(1, 2, 3, 4, 5, 6, 7)
    val isDaily = selectedDays == allDays
    val hours12 = (1..12).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }
    val amPm = listOf("AM", "PM")

    val hour24 = when {
        isAm && hour12 == 12 -> 0
        !isAm && hour12 == 12 -> 12
        !isAm -> hour12 + 12
        else -> hour12
    }

    LaunchedEffect(alarmId) {
        // GUARD: if already initialized, do NOT reload from DB.
        // Without this, returning from any child screen (SnoozeSettings,
        // AlarmSound, GentleWakeUp, etc.) causes Navigation Compose to
        // relaunch this LaunchedEffect, reloading the DB and wiping every
        // unsaved change the user made.
        if (screenReady) return@LaunchedEffect

        if (isEditing) {
            viewModel.getAlarmById(alarmId)?.let { alarm ->
                val h = alarm.hour
                hour12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                isAm = h < 12
                minute = alarm.minute
                prevMinuteIdx = alarm.minute   // sync carry-over tracker
                // Parse emoji prefix from saved label (e.g. "⏰ Morning" → emoji="⏰", label="Morning")
                val rawLabel = alarm.label
                val startsWithEmoji = rawLabel.isNotEmpty() &&
                    (rawLabel[0].code > 255 || Character.isHighSurrogate(rawLabel[0]))
                if (startsWithEmoji) {
                    val spaceIdx = rawLabel.indexOf(' ')
                    if (spaceIdx > 0) {
                        labelEmoji = rawLabel.substring(0, spaceIdx)
                        label     = rawLabel.substring(spaceIdx + 1)
                    } else {
                        labelEmoji = rawLabel
                        label     = ""
                    }
                } else {
                    labelEmoji = ""
                    label      = rawLabel
                }
                vibrate = alarm.vibrate
                selectedDays = alarm.repeatDays
                    .split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                wakeUpTasks = alarm.tasks.toWakeUpTasks()
                riseCheckMinutes = if (alarm.riseCheckMinutes > 0) alarm.riseCheckMinutes else null
                selectedSound = alarm.sound
                alarmVolume = alarm.volume
                gentleWakeUpSeconds = alarm.gentleWakeUpSeconds
                timeAnnouncement = alarm.timeAnnouncement
                announcementVoice = alarm.announcementVoice
                weatherReminder = alarm.weatherReminder
                labelReminder = alarm.labelReminder
                extraLoud = alarm.extraLoud
                snoozeEnabled = alarm.snoozeEnabled
                snoozeIntervalMinutes = alarm.snoozeIntervalMinutes
                snoozeLimit = alarm.snoozeLimit
                snoozeProgressiveMode = alarm.snoozeProgressiveMode
            }
        }
        screenReady = true   // mark ready — changes after this point are real edits
    }

    // Watch all key fields — flag as unsaved after initialization
    LaunchedEffect(label, hour12, minute, isAm, selectedDays, vibrate,
        selectedSound, alarmVolume, wakeUpTasks, snoozeEnabled,
        gentleWakeUpSeconds, extraLoud, timeAnnouncement) {
        if (screenReady) hasChanges = true
    }

    val ringInText = remember(hour24, minute) {
        val now = Calendar.getInstance()
        val currentTotalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 +
                now.get(Calendar.MINUTE)
        val alarmTotalMinutes = hour24 * 60 + minute
        val diffMinutes = if (alarmTotalMinutes > currentTotalMinutes) {
            alarmTotalMinutes - currentTotalMinutes
        } else {
            1440 - currentTotalMinutes + alarmTotalMinutes
        }
        val hrs = diffMinutes / 60
        val mins = diffMinutes % 60
        when {
            hrs == 0 -> "Ring in $mins min"
            mins == 0 -> "Ring in $hrs hr"
            else -> "Ring in $hrs hr $mins min"
        }
    }

    // Live preview text — "Rings Tuesday, 7:00 AM · in 14h 32m"
    val nextRingPreviewText = remember(hour24, minute, selectedDays) {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!cal.after(now)) cal.add(Calendar.DAY_OF_MONTH, 1)
        if (selectedDays.isNotEmpty()) {
            for (i in 0..6) {
                val check = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, i)
                    set(Calendar.HOUR_OF_DAY, hour24)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (check.after(now) && check.get(Calendar.DAY_OF_WEEK) in selectedDays) {
                    cal.timeInMillis = check.timeInMillis; break
                }
            }
        }
        val dayNames = arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val today    = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val prefix   = when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
            else -> dayNames[cal.get(Calendar.DAY_OF_WEEK)]
        }
        "Rings $prefix, ${formatTime(hour24, minute)}"
    }


    val currentTone = allAlarmTones.find { it.rawResName == selectedSound }
        ?: AlarmTone("alarm_digital_alarm", "Digital Alarm", "Emergency")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),  // room for sticky save button
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = if (isEditing) "Edit Alarm" else "New Alarm",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                Box {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Primary,
                        )
                    }
                    // Red dot when there are unsaved changes
                    if (hasChanges) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .clip(CircleShape)
                                .background(Danger),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // ─── Label ────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            placeholder = {
                Text(
                    text = "Alarm label...",
                    color = TextMuted,
                    fontFamily = MulishFamily,
                    fontSize = 16.sp,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Primary,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = MulishFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
            ),
            leadingIcon = {
                IconButton(onClick = { showEmojiPicker = true }) {
                    if (labelEmoji.isNotEmpty()) {
                        Text(
                            text = labelEmoji,
                            fontSize = 24.sp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.EmojiEmotions,
                            contentDescription = "Emoji",
                            tint = Primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Preview card ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Primary.copy(alpha = 0.10f))
                .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = nextRingPreviewText,
                        fontSize = 14.sp, fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily, color = TextPrimary,
                    )
                    Text(
                        text = ringInText,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = Primary,
                    )
                }
                Text("🔔", fontSize = 22.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Time Picker ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(2f)) {
                    WheelPicker(
                        items         = hours12,
                        initialIndex  = hour12 - 1,
                        circular      = true,
                        controller    = hourController,
                        onItemSelected = { hour12 = it + 1 },
                    )
                }
                Text(
                    text = ":",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color = Primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Box(modifier = Modifier.weight(2f)) {
                    WheelPicker(
                        items         = minutes,
                        initialIndex  = minute,
                        circular      = true,
                        onItemSelected = { newIdx ->
                            val prev = prevMinuteIdx
                            prevMinuteIdx = newIdx
                            minute = newIdx

                            when {
                                // ── Forward rollover: 59 → 00 ─────────────────
                                // Hour ticks forward. If hour was 12, AM/PM flips.
                                prev == 59 && newIdx == 0 -> {
                                    val flipsAmPm = (hour12 == 12)
                                    val nextHour  = if (hour12 == 12) 1 else hour12 + 1
                                    hour12 = nextHour
                                    if (flipsAmPm) {
                                        isAm = !isAm
                                        amPmController.scrollTo(if (isAm) 0 else 1)
                                    }
                                    hourController.scrollTo(nextHour - 1)
                                }
                                // ── Backward rollover: 00 → 59 ────────────────
                                // Hour ticks back. If hour was 1, AM/PM flips.
                                prev == 0 && newIdx == 59 -> {
                                    val flipsAmPm = (hour12 == 1)
                                    val prevHour  = if (hour12 == 1) 12 else hour12 - 1
                                    hour12 = prevHour
                                    if (flipsAmPm) {
                                        isAm = !isAm
                                        amPmController.scrollTo(if (isAm) 0 else 1)
                                    }
                                    hourController.scrollTo(prevHour - 1)
                                }
                            }
                        },
                    )
                }
                Box(modifier = Modifier.weight(1.5f)) {
                    WheelPicker(
                        items         = amPm,
                        initialIndex  = if (isAm) 0 else 1,
                        circular      = false,
                        controller    = amPmController,
                        onItemSelected = { isAm = it == 0 },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Section header ─────────────────────────────────────────────────────
        AlarmSectionHeader(icon = "📅", title = "SCHEDULE")
        Spacer(modifier = Modifier.height(8.dp))

        // ─── Repeat Days ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = getSmartRepeatLabel(selectedDays),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = if (selectedDays.isEmpty()) TextMuted else Primary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        selectedDays = if (isDaily) emptySet() else allDays
                    },
                ) {
                    Icon(
                        imageVector = if (isDaily) Icons.Filled.CheckBox
                        else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = "Daily",
                        tint = if (isDaily) Primary else TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Daily",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = if (isDaily) Primary else TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val days = listOf(
                2 to "M", 3 to "T", 4 to "W",
                5 to "T", 6 to "F", 7 to "S", 1 to "S",
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
                            .background(if (isSelected) Primary else SurfaceHigh)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Primary else Border,
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedDays = if (isSelected) selectedDays - dayNum
                                else selectedDays + dayNum
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Section header ─────────────────────────────────────────────────────
        AlarmSectionHeader(icon = "⚡", title = "WAKE-UP TASKS")
        Spacer(modifier = Modifier.height(8.dp))

        // ─── Wake-Up Tasks ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
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
                    Text(
                        text = "Wake-Up Tasks",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Complete tasks to dismiss alarm",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                    )
                }
                Text(
                    text = "${wakeUpTasks.size}/5",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = if (wakeUpTasks.isEmpty()) TextMuted else Primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in 0 until 5) {
                    val task = wakeUpTasks.getOrNull(i)

                    // Outer box is taller than the visible card so the ✕ badge
                    // can overhang the top-right corner without clipping
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                    ) {
                        // ── Main task card ────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (task != null) Primary.copy(alpha = 0.15f)
                                    else SurfaceHigh,
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (task != null) Primary else Border,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    if (task != null) {
                                        // Tap existing task → edit/replace mode
                                        editingTaskIndex = i
                                        showTaskPicker = true
                                    } else if (wakeUpTasks.size < 5) {
                                        // Tap empty slot → add mode
                                        editingTaskIndex = null
                                        showTaskPicker = true
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (task != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(text = task.emoji(), fontSize = 22.sp)
                                    Text(
                                        text = task.title(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily,
                                        color = Primary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                Text(
                                    text = "+",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = TextMuted.copy(alpha = 0.4f),
                                )
                            }
                        }

                        // ── ✕ remove badge (only for filled slots) ────────────
                        if (task != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(0xFFEF4444))
                                    .clickable {
                                        wakeUpTasks = wakeUpTasks
                                            .toMutableList()
                                            .also { list -> list.removeAt(i) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove task",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(10.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (wakeUpTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap to edit  ·  ✕ to remove",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Are You Up? ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(14.dp))
                .clickable {
                    riseCheckMinutes = when (riseCheckMinutes) {
                        null -> 5
                        5 -> 10
                        10 -> 15
                        else -> null
                    }
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Are You Up?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Danger.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "HOT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = Danger,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                Text(
                    text = "Re-check if you're awake after dismissal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                )
            }
            Text(
                text = if (riseCheckMinutes != null) "${riseCheckMinutes}m ›" else "Off ›",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = if (riseCheckMinutes != null) Primary else TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Section header ─────────────────────────────────────────────────────
        AlarmSectionHeader(icon = "🔔", title = "RING BEHAVIOUR")
        Spacer(modifier = Modifier.height(8.dp))

        // ─── Alarm Sound + Volume + Vibrate + Gentle Wake-Up + Announcements ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            // ─── Play + Name + Arrow ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isPreviewPlaying) Primary else SurfaceHigh)
                        .clickable {
                            if (isPreviewPlaying) {
                                previewPlayer?.apply { if (isPlaying) stop(); release() }
                                previewPlayer = null
                                isPreviewPlaying = false
                            } else {
                                val resId = context.resources.getIdentifier(
                                    selectedSound, "raw", context.packageName
                                )
                                if (resId != 0) {
                                    previewPlayer = MediaPlayer.create(context, resId)?.apply {
                                        setOnCompletionListener {
                                            isPreviewPlaying = false
                                            release()
                                            previewPlayer = null
                                        }
                                        start()
                                    }
                                    isPreviewPlaying = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Filled.Pause
                        else Icons.Filled.PlayArrow,
                        contentDescription = "Preview",
                        tint = if (isPreviewPlaying) Color.White else TextMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Text(
                    text = currentTone.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        navController.navigate(Screen.AlarmSound.createRoute(selectedSound))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = "Choose Sound",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Volume Slider ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "🔈", fontSize = 18.sp)
                Slider(
                    value = alarmVolume,
                    onValueChange = { alarmVolume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceHigh,
                    ),
                )
                Text(text = "🔊", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Vibrate Toggle ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Vibrate",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Phone vibrates when alarm fires",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = vibrate,
                    onCheckedChange = { vibrate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceHigh,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Gentle Wake-Up Row ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceHigh)
                    .clickable {
                        navController.navigate(
                            Screen.GentleWakeUp.createRoute(gentleWakeUpSeconds)
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // ── Left: icon badge + label ──────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                ) {
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (gentleWakeUpSeconds > 0)
                                    Primary.copy(alpha = 0.12f)
                                else
                                    TextMuted.copy(alpha = 0.07f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (gentleWakeUpSeconds > 0) Primary
                                   else TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Gentle Wake-Up",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Gradually increase volume from silence",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                }

                // ── Right: premium value pill + chevron (top-aligned) ─────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (gentleWakeUpSeconds > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Primary.copy(alpha = 0.13f))
                                .border(
                                    1.dp,
                                    Primary.copy(alpha = 0.28f),
                                    RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(11.dp),
                                )
                                Text(
                                    text = gentleWakeUpLabel(gentleWakeUpSeconds),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = Primary,
                                    letterSpacing = 0.2.sp,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = TextMuted.copy(alpha = 0.45f),
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Time Announcement ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time Announcement",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Announces greeting + date + time on alarm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = timeAnnouncement,
                        onCheckedChange = { timeAnnouncement = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }

                if (timeAnnouncement) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(
                            "female" to "Female",
                            "male" to "Male",
                        ).forEach { (voice, voiceLabel) ->
                            val isSelected = announcementVoice == voice
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Primary else CardBackground)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) Primary else Border,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { announcementVoice = voice },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = voiceLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = if (isSelected) Color.White else TextMuted,
                                )
                            }
                        }

                        // ─── Time Sample Button ───────────────────────────────
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSamplePlaying) Danger.copy(alpha = 0.15f)
                                    else Primary.copy(alpha = 0.15f),
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSamplePlaying) Danger else Primary,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    if (isSamplePlaying) {
                                        sampleTts?.stop()
                                        sampleTts?.shutdown()
                                        sampleTts = null
                                        isSamplePlaying = false
                                    } else {
                                        isSamplePlaying = true
                                        var newTts: TextToSpeech? = null
                                        newTts = TextToSpeech(context) { status ->
                                            if (status == TextToSpeech.SUCCESS) {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    when (announcementVoice) {
                                                        "male" -> {
                                                            newTts?.setPitch(0.6f)
                                                            newTts?.setSpeechRate(0.9f)
                                                        }
                                                        else -> {
                                                            newTts?.setPitch(1.8f)
                                                            newTts?.setSpeechRate(0.9f)
                                                        }
                                                    }
                                                    newTts?.language = Locale.US
                                                    newTts?.setOnUtteranceProgressListener(
                                                        object : UtteranceProgressListener() {
                                                            override fun onStart(id: String?) {}
                                                            override fun onDone(id: String?) {
                                                                Handler(Looper.getMainLooper()).post {
                                                                    isSamplePlaying = false
                                                                    newTts?.shutdown()
                                                                    sampleTts = null
                                                                }
                                                            }
                                                            override fun onError(id: String?) {
                                                                Handler(Looper.getMainLooper()).post {
                                                                    isSamplePlaying = false
                                                                    newTts?.shutdown()
                                                                    sampleTts = null
                                                                }
                                                            }
                                                        },
                                                    )
                                                    val params = Bundle()
                                                    params.putString(
                                                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                                        "sample",
                                                    )
                                                    newTts?.speak(
                                                        buildSampleAnnouncementText(),
                                                        TextToSpeech.QUEUE_FLUSH,
                                                        params,
                                                        "sample",
                                                    )
                                                }, 300)
                                            } else {
                                                isSamplePlaying = false
                                            }
                                        }
                                        sampleTts = newTts
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = if (isSamplePlaying) Icons.Filled.Pause
                                    else Icons.Filled.PlayArrow,
                                    contentDescription = "Sample",
                                    tint = if (isSamplePlaying) Danger else Primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (isSamplePlaying) "Stop" else "Sample",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = if (isSamplePlaying) Danger else Primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Weather Reminder ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weather Reminder",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Announces weather conditions on alarm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = weatherReminder,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // ✅ SET TRUE IMMEDIATELY — toggle stays ON no matter what
                                weatherReminder = true

                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED

                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine || hasCoarse) {
                                    // Permission already granted → fetch directly
                                    fetchLocation()
                                } else {
                                    // Ask for permission
                                    // Toggle stays ON — only turns OFF if user explicitly denies
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    )
                                }
                            } else {
                                weatherReminder = false
                                locationStatus = "idle"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }

                // ─── Location status ──────────────────────────────────────────
                if (weatherReminder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (locationStatus) {
                                    "saved" -> Primary.copy(alpha = 0.1f)
                                    "failed" -> Danger.copy(alpha = 0.1f)
                                    else -> CardBackground
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (locationStatus) {
                            "fetching" -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = "Fetching your location...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = TextMuted,
                                )
                            }
                            "saved" -> {
                                Text(text = "📍", fontSize = 14.sp)
                                Text(
                                    text = "Location saved! Weather ready",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = Primary,
                                )
                            }
                            "failed" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(text = "❌", fontSize = 14.sp)
                                        Text(
                                            text = "GPS not found. Please turn on Location.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MulishFamily,
                                            color = Danger,
                                        )
                                    }
                                    // ─── Open Location Settings ───────────────
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Danger.copy(alpha = 0.1f))
                                            .border(1.dp, Danger, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                                )
                                                context.startActivity(intent)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Turn On Location Services",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily,
                                            color = Danger,
                                        )
                                    }
                                    // ─── Retry button ─────────────────────────
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Primary.copy(alpha = 0.1f))
                                            .border(1.dp, Primary, RoundedCornerShape(10.dp))
                                            .clickable { fetchLocation() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Retry",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily,
                                            color = Primary,
                                        )
                                    }
                                }
                            }
                            "permission_needed" -> {
                                Text(text = "⚙️", fontSize = 14.sp)
                                Text(
                                    text = "Allow location in Settings → Apps → Ontime → Permissions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = Danger,
                                )
                            }
                            else -> {
                                Text(text = "📡", fontSize = 14.sp)
                                Text(
                                    text = "Waiting for location...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = MulishFamily,
                                    color = TextMuted,
                                )
                            }
                        }
                    }

                    // ─── Open Settings button (when permission needed) ─────────
                    if (locationStatus == "permission_needed") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Danger.copy(alpha = 0.1f))
                                .border(1.dp, Danger, RoundedCornerShape(10.dp))
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = android.net.Uri.fromParts(
                                            "package", context.packageName, null
                                        )
                                    }
                                    context.startActivity(intent)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Open App Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = Danger,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Primary.copy(alpha = 0.1f))
                                .border(1.dp, Primary, RoundedCornerShape(10.dp))
                                .clickable {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        fetchLocation()
                                    } else {
                                        locationStatus = "permission_needed"
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Retry After Granting Permission",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = Primary,
                            )
                        }
                    }

                    // ─── Weather Sample Button ────────────────────────────────
                    if (locationStatus == "saved") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isWeatherSamplePlaying) Danger.copy(alpha = 0.15f)
                                    else Primary.copy(alpha = 0.15f),
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isWeatherSamplePlaying) Danger else Primary,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    if (isWeatherSamplePlaying) {
                                        weatherSampleTts?.stop()
                                        weatherSampleTts?.shutdown()
                                        weatherSampleTts = null
                                        isWeatherSamplePlaying = false
                                    } else {
                                        isWeatherSamplePlaying = true
                                        val sampleWeatherText =
                                            "Here is today's weather update. " +
                                                    "It's a beautiful sunny day outside! " +
                                                    "Current temperature is 28 degrees Celsius. " +
                                                    "Today's high is 32 degrees " +
                                                    "and the low is 22 degrees. " +
                                                    "Air quality is good today."

                                        var newTts: TextToSpeech? = null
                                        newTts = TextToSpeech(context) { status ->
                                            if (status == TextToSpeech.SUCCESS) {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    when (announcementVoice) {
                                                        "male" -> {
                                                            newTts?.setPitch(0.6f)
                                                            newTts?.setSpeechRate(0.9f)
                                                        }
                                                        else -> {
                                                            newTts?.setPitch(1.8f)
                                                            newTts?.setSpeechRate(0.9f)
                                                        }
                                                    }
                                                    newTts?.language = Locale.US
                                                    newTts?.setOnUtteranceProgressListener(
                                                        object : UtteranceProgressListener() {
                                                            override fun onStart(id: String?) {}
                                                            override fun onDone(id: String?) {
                                                                Handler(Looper.getMainLooper()).post {
                                                                    isWeatherSamplePlaying = false
                                                                    newTts?.shutdown()
                                                                    weatherSampleTts = null
                                                                }
                                                            }
                                                            override fun onError(id: String?) {
                                                                Handler(Looper.getMainLooper()).post {
                                                                    isWeatherSamplePlaying = false
                                                                    newTts?.shutdown()
                                                                    weatherSampleTts = null
                                                                }
                                                            }
                                                        },
                                                    )
                                                    val params = Bundle()
                                                    params.putString(
                                                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                                        "weather_sample",
                                                    )
                                                    newTts?.speak(
                                                        sampleWeatherText,
                                                        TextToSpeech.QUEUE_FLUSH,
                                                        params,
                                                        "weather_sample",
                                                    )
                                                }, 300)
                                            } else {
                                                isWeatherSamplePlaying = false
                                            }
                                        }
                                        weatherSampleTts = newTts
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = if (isWeatherSamplePlaying) Icons.Filled.Pause
                                    else Icons.Filled.PlayArrow,
                                    contentDescription = "Weather Sample",
                                    tint = if (isWeatherSamplePlaying) Danger else Primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (isWeatherSamplePlaying) "Stop" else "Sample",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = if (isWeatherSamplePlaying) Danger else Primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Label Reminder ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // Toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Label Reminder",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Reads your alarm label out loud",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = labelReminder,
                        onCheckedChange = { labelReminder = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }

                // Sample button (visible when ON)
                if (labelReminder) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val sampleLabelText = label.ifBlank { "No label set. Please add a label first." }
                    var isLabelSamplePlaying by remember { mutableStateOf(false) }
                    var labelSampleTts by remember { mutableStateOf<TextToSpeech?>(null) }

                    // Show what will be spoken
                    if (label.isNotBlank()) {
                        Text(
                            text = "Will say: \"$label\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = Primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    } else {
                        Text(
                            text = "Add a label above to use this feature",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    // Sample button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isLabelSamplePlaying) Danger.copy(alpha = 0.15f)
                                else Primary.copy(alpha = 0.15f),
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isLabelSamplePlaying) Danger else Primary,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                if (isLabelSamplePlaying) {
                                    labelSampleTts?.stop()
                                    labelSampleTts?.shutdown()
                                    labelSampleTts = null
                                    isLabelSamplePlaying = false
                                } else {
                                    isLabelSamplePlaying = true
                                    var newTts: TextToSpeech? = null
                                    newTts = TextToSpeech(context) { status ->
                                        if (status == TextToSpeech.SUCCESS) {
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                when (announcementVoice) {
                                                    "male" -> {
                                                        newTts?.setPitch(0.6f)
                                                        newTts?.setSpeechRate(0.9f)
                                                    }
                                                    else -> {
                                                        newTts?.setPitch(1.8f)
                                                        newTts?.setSpeechRate(0.9f)
                                                    }
                                                }
                                                newTts?.language = Locale.US
                                                newTts?.setOnUtteranceProgressListener(
                                                    object : UtteranceProgressListener() {
                                                        override fun onStart(id: String?) {}
                                                        override fun onDone(id: String?) {
                                                            Handler(Looper.getMainLooper()).post {
                                                                isLabelSamplePlaying = false
                                                                newTts?.shutdown()
                                                                labelSampleTts = null
                                                            }
                                                        }
                                                        override fun onError(id: String?) {
                                                            Handler(Looper.getMainLooper()).post {
                                                                isLabelSamplePlaying = false
                                                                newTts?.shutdown()
                                                                labelSampleTts = null
                                                            }
                                                        }
                                                    },
                                                )
                                                val params = Bundle()
                                                params.putString(
                                                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                                    "label_sample",
                                                )
                                                newTts?.speak(
                                                    sampleLabelText,
                                                    TextToSpeech.QUEUE_FLUSH,
                                                    params,
                                                    "label_sample",
                                                )
                                            }, 300)
                                        } else {
                                            isLabelSamplePlaying = false
                                        }
                                    }
                                    labelSampleTts = newTts
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (isLabelSamplePlaying) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                                contentDescription = "Label Sample",
                                tint = if (isLabelSamplePlaying) Danger else Primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (isLabelSamplePlaying) "Stop" else "Sample",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = if (isLabelSamplePlaying) Danger else Primary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Extra Loud Effect ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (extraLoud) Danger.copy(alpha = 0.08f) else SurfaceHigh,
                )
                .border(
                    width = if (extraLoud) 1.5.dp else 0.dp,
                    color = if (extraLoud) Danger.copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (extraLoud) "💥 Extra Loud Effect"
                        else "🔊 Extra Loud Effect",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (extraLoud) Danger else TextPrimary,
                    )
                    // ─── ? info button ────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CardBackground)
                            .border(1.dp, Border, CircleShape)
                            .clickable {
                                navController.navigate(
                                    Screen.ExtraLoudInfo.createRoute(selectedSound)
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "?",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                }
                Text(
                    text = if (extraLoud) "Blasts aggressive sounds after 35 sec!"
                    else "For heavy sleepers — blasts at MAX volume",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = if (extraLoud) Danger.copy(alpha = 0.8f) else TextMuted,
                )
            }
            Switch(
                checked = extraLoud,
                onCheckedChange = { extraLoud = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Danger,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceHigh,
                ),
            )
        }

        // ─── Task Picker Sheet ────────────────────────────────────────────────
        // ─── Emoji Picker Sheet ───────────────────────────────────────────────
        if (showEmojiPicker) {
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showEmojiPicker = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = CardBackground,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Choose Emoji",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Tap to add to your alarm label",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color = TextMuted,
                            )
                        }
                        if (label.isNotEmpty() || labelEmoji.isNotEmpty()) {
                            // Clear emoji button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceHigh)
                                    .clickable {
                                        labelEmoji = ""
                                        showEmojiPicker = false
                                        focusRequester.requestFocus()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "Clear emoji",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = TextMuted,
                                )
                            }
                        }
                    }

                    // Emoji categories
                    val emojiRows = listOf(
                        "⏰ 🌅 ☀️ 🌙 🌝 💤 🛌 🌊 🌿 ☕",
                        "💪 🏃 🧘 🏋️ 🚴 🏊 🤸 🧗 🎯 🔥",
                        "📚 💼 🎵 🎮 🎉 ✨ 💫 🌈 🏠 🚗",
                        "🍎 🥗 💊 🧠 🔔 ✈️ 🎓 💰 🌺 ⚡",
                    )
                    val allEmojis = emojiRows.flatMap { row ->
                        row.split(" ").filter { it.isNotBlank() }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(allEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceHigh)
                                    .clickable {
                                        labelEmoji = emoji
                                        showEmojiPicker = false
                                        focusRequester.requestFocus()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTaskPicker) {
            WakeUpTaskPickerSheet(
                onTaskSelected = { task ->
                    val idx = editingTaskIndex
                    wakeUpTasks = if (idx != null) {
                        // Replace existing task at editingTaskIndex
                        wakeUpTasks.toMutableList().also { it[idx] = task }
                    } else {
                        // Append new task
                        wakeUpTasks.toMutableList().also { it.add(task) }
                    }
                    editingTaskIndex = null
                    showTaskPicker = false
                },
                onDismiss = {
                    editingTaskIndex = null
                    showTaskPicker = false
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Custom Settings label ────────────────────────────────────────────
        Text(
            text = "CUSTOM SETTINGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Snooze Row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .clickable {
                    navController.navigate(
                        Screen.SnoozeSettings.createRoute(
                            snoozeEnabled, snoozeIntervalMinutes,
                            snoozeLimit, snoozeProgressiveMode,
                        )
                    )
                }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "💤 Snooze",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                Text(
                    text = buildSnoozeSummary(
                        snoozeEnabled, snoozeIntervalMinutes,
                        snoozeLimit, snoozeProgressiveMode,
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = if (snoozeEnabled) Primary else TextMuted,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = "Snooze Settings",
                tint = TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    } // end scrollable Column

    // ── Sticky gradient save button ────────────────────────────────────────────
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background.copy(alpha = 0f), Background, Background),
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(listOf(Primary, Color(0xFF9333EA)))
                )
                .clickable {
                    if (isEditing) {
                        viewModel.updateAlarmById(
                            alarmId = alarmId,
                            hour = hour24, minute = minute,
                            label = "$labelEmoji $label".trim().ifEmpty { "Alarm" },
                            repeatDays = selectedDays.sorted().joinToString(","),
                            vibrate = vibrate, tasks = wakeUpTasks,
                            riseCheckMinutes = riseCheckMinutes ?: 0,
                            sound = selectedSound, volume = alarmVolume,
                            gentleWakeUpSeconds = gentleWakeUpSeconds,
                            timeAnnouncement = timeAnnouncement,
                            announcementVoice = announcementVoice,
                            weatherReminder = weatherReminder,
                            labelReminder = labelReminder, extraLoud = extraLoud,
                            snoozeEnabled = snoozeEnabled,
                            snoozeIntervalMinutes = snoozeIntervalMinutes,
                            snoozeLimit = snoozeLimit,
                            snoozeProgressiveMode = snoozeProgressiveMode,
                        )
                    } else {
                        viewModel.createAlarm(
                            hour = hour24, minute = minute,
                            label = "$labelEmoji $label".trim().ifEmpty { "Alarm" },
                            repeatDays = selectedDays.sorted().joinToString(","),
                            vibrate = vibrate, tasks = wakeUpTasks,
                            riseCheckMinutes = riseCheckMinutes ?: 0,
                            sound = selectedSound, volume = alarmVolume,
                            gentleWakeUpSeconds = gentleWakeUpSeconds,
                            timeAnnouncement = timeAnnouncement,
                            announcementVoice = announcementVoice,
                            weatherReminder = weatherReminder,
                            labelReminder = labelReminder, extraLoud = extraLoud,
                            snoozeEnabled = snoozeEnabled,
                            snoozeIntervalMinutes = snoozeIntervalMinutes,
                            snoozeLimit = snoozeLimit,
                            snoozeProgressiveMode = snoozeProgressiveMode,
                        )
                    }
                    navController.popBackStack()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isEditing) "💾" else "🔔",
                    fontSize = 18.sp,
                )
                Text(
                    text = if (isEditing) "Save Changes" else "Create Alarm",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = Color.White,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
    } // end outer Box
}

// ─── Section header composable ────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun AlarmSectionHeader(icon: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            text  = title,
            fontSize   = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = com.tushartamrakar.ontime.core.ui.theme.TextMuted,
            letterSpacing = 1.2.sp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(com.tushartamrakar.ontime.core.ui.theme.Border.copy(alpha = 0.3f))
        )
    }
}
