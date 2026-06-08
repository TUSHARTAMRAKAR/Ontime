package com.tushartamrakar.ontime.calendar.presentation

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
import com.tushartamrakar.ontime.calendar.domain.RecurrenceType
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import com.tushartamrakar.ontime.calendar.data.local.toAttendee
import com.tushartamrakar.ontime.calendar.notification.AttendeeNotificationHelper
import com.tushartamrakar.ontime.calendar.notification.formatEventDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavHostController,
    initialDate: String = "",
    eventId: Int = -1,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val categories by viewModel.allCategories.collectAsState()
    val isEditing = eventId != -1

    // ─── Parse initial date ───────────────────────────────────────────────────
    val parsedDate = remember(initialDate) {
        try {
            if (initialDate.isNotBlank()) LocalDate.parse(initialDate) else LocalDate.now()
        } catch (e: Exception) { LocalDate.now() }
    }

    // ─── State ────────────────────────────────────────────────────────────────
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(1) }
    var startDate by remember { mutableStateOf(parsedDate) }
    var endDate by remember { mutableStateOf(parsedDate) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var isAllDay by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.NONE) }
    var reminderType by remember { mutableStateOf("NONE") }
    var reminderMinutesBefore by remember { mutableStateOf(10) }
    var reminderSound by remember { mutableStateOf("alarm_digital_alarm") }
    var announceLabelOnReminder by remember { mutableStateOf(false) }

    // ─── Add people state ─────────────────────────────────────────────────────
    // ── Single source of truth — shared with AddPeopleScreen via singleton ──
    val attendees = viewModel.draftAttendees
    var showRenotifyDialog by remember { mutableStateOf(false) }
    // Snapshot of attendees when edit screen opened — to detect changes on save
    var originalAttendees by remember { mutableStateOf<List<EventAttendeeEntity>>(emptyList()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Runtime permission launchers
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* nothing — search field handles gracefully if denied */ }
    // ─── Load existing event in edit mode ────────────────────────────────────
    val allEvents by viewModel.allEvents.collectAsState()
    LaunchedEffect(eventId, allEvents) {
        if (isEditing) {
            val existing = allEvents.find { it.id == eventId } ?: return@LaunchedEffect
            title = existing.title
            description = existing.description
            location = existing.location
            selectedCategoryId = existing.categoryId
            isAllDay = existing.isAllDay
            recurrenceType = try {
                RecurrenceType.valueOf(existing.recurrenceType)
            } catch (e: Exception) { RecurrenceType.NONE }
            reminderType = existing.reminderType
            reminderMinutesBefore = existing.reminderMinutesBefore
            reminderSound = existing.reminderSound
            announceLabelOnReminder = existing.announceLabelOnReminder
            val zone = ZoneId.systemDefault()
            val startZdt = Instant.ofEpochMilli(existing.startTimeMillis).atZone(zone)
            val endZdt = Instant.ofEpochMilli(existing.endTimeMillis).atZone(zone)
            startDate = startZdt.toLocalDate()
            endDate = endZdt.toLocalDate()
            startTime = startZdt.toLocalTime()
            endTime = endZdt.toLocalTime()
        }
    }

    // ─── Contact search with 300 ms debounce ─────────────────────────────────
    // ─── Initialise the draft store ONCE per screen instance ─────────────────
    // rememberSaveable persists the flag across recompositions caused by sub-
    // navigation (AddPeopleScreen). So:
    //   • First open of this CreateEventScreen → clear store + load edit data
    //   • Return from AddPeopleScreen          → flag is true, skip (preserves
    //     whatever AddPeopleScreen just added)
    val initialized = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(eventId, initialized.value) {
        if (!initialized.value) {
            viewModel.draftAttendees.clear()
            if (isEditing && eventId != -1) {
                val existing = viewModel.getAttendeesOnce(eventId)
                viewModel.draftAttendees.addAll(existing)
                originalAttendees = existing
            }
            initialized.value = true
        }
    }

    // ─── Receive selected sound back from AlarmSoundScreen ───────────────────
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow<String?>("selected_sound", null)?.collect { sound ->
                if (sound != null) {
                    reminderSound = sound
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_sound")
                }
            }
    }

    // ─── Dialog state ─────────────────────────────────────────────────────────
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun toMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val categoryColor = selectedCategory?.let { parseColor(it.colorHex) } ?: Primary

    // ─── UI ───────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close",
                        tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = if (isEditing) "Edit Event" else "New Event",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextPrimary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            if (isEditing) {
                                // ── UPDATE existing event ──────────────────────
                                val existing = allEvents.find { it.id == eventId }
                                if (existing != null) {
                                    viewModel.updateEvent(
                                        existing.copy(
                                            title = title,
                                            description = description,
                                            location = location,
                                            categoryId = selectedCategoryId,
                                            startTimeMillis = toMillis(startDate, if (isAllDay) LocalTime.MIDNIGHT else startTime),
                                            endTimeMillis = toMillis(endDate, if (isAllDay) LocalTime.of(23, 59) else endTime),
                                            isAllDay = isAllDay,
                                            recurrenceType = recurrenceType.name,
                                            reminderType = reminderType,
                                            reminderMinutesBefore = reminderMinutesBefore,
                                            reminderSound = reminderSound,
                                            announceLabelOnReminder = announceLabelOnReminder,
                                        )
                                    )
                                    // Save attendees always; ask about renotifying
                                    val newlyAdded = attendees.filter { a ->
                                        originalAttendees.none { o -> o.name == a.name }
                                    }
                                    viewModel.saveAttendees(eventId, attendees.toList())
                                    if (newlyAdded.isNotEmpty()) {
                                        showRenotifyDialog = true
                                    } else {
                                        navController.navigateUp()
                                    }
                                }
                            } else {
                                // ── CREATE new event + save attendees ──────────
                                val startMillis = toMillis(startDate, if (isAllDay) LocalTime.MIDNIGHT else startTime)
                                val endMillis   = toMillis(endDate, if (isAllDay) LocalTime.of(23, 59) else endTime)
                                scope.launch {
                                    viewModel.createEventWithAttendees(
                                        title = title,
                                        description = description,
                                        location = location,
                                        categoryId = selectedCategoryId,
                                        startTimeMillis = startMillis,
                                        endTimeMillis   = endMillis,
                                        isAllDay = isAllDay,
                                        recurrenceType = recurrenceType,
                                        reminderType = reminderType,
                                        reminderMinutesBefore = reminderMinutesBefore,
                                        reminderSound = reminderSound,
                                        announceLabelOnReminder = announceLabelOnReminder,
                                        attendees = attendees.toList(),
                                    )
                                }
                                navController.navigateUp()
                            }
                        }
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Text(
                        text = "Save",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (title.isNotBlank()) Primary else TextMuted,
                    )
                }
            }

            // ─── Scrollable Content ───────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // ── Title ─────────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = TextPrimary, fontSize = 20.sp,
                            fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold,
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(text = "Event title", color = TextMuted, fontSize = 20.sp,
                                    fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold)
                            }
                            innerTextField()
                        },
                    )
                }

                // ── Category ──────────────────────────────────────────────────
                EventFormRow(
                    icon = null,
                    label = "Category",
                    onClick = { showCategoryPicker = true },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selectedCategory != null) {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                                    .background(categoryColor),
                            )
                            Text(
                                text = "${selectedCategory.emoji} ${selectedCategory.name}",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily, color = categoryColor,
                            )
                        }
                    }
                }

                // ── All Day Toggle ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "All Day", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = TextPrimary)
                    Switch(
                        checked = isAllDay, onCheckedChange = { isAllDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary, checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }

                // ── Start Date & Time ─────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp)),
                ) {
                    EventFormRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "Start",
                        onClick = { showStartDatePicker = true },
                    ) {
                        Text(text = startDate.format(dateFormatter), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                    }
                    if (!isAllDay) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                        EventFormRow(
                            icon = Icons.Filled.AccessTime,
                            label = "Start time",
                            onClick = { showStartTimePicker = true },
                        ) {
                            Text(text = startTime.format(timeFormatter), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                        }
                    }
                }

                // ── End Date & Time ───────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp)),
                ) {
                    EventFormRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "End",
                        onClick = { showEndDatePicker = true },
                    ) {
                        Text(text = endDate.format(dateFormatter), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                    }
                    if (!isAllDay) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                        EventFormRow(
                            icon = Icons.Filled.AccessTime,
                            label = "End time",
                            onClick = { showEndTimePicker = true },
                        ) {
                            Text(text = endTime.format(timeFormatter), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Description, contentDescription = null,
                            tint = TextMuted, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        BasicTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp,
                                fontFamily = MulishFamily, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(Primary),
                            decorationBox = { innerTextField ->
                                if (description.isEmpty()) {
                                    Text(text = "Description (optional)", color = TextMuted,
                                        fontSize = 14.sp, fontFamily = MulishFamily)
                                }
                                innerTextField()
                            },
                        )
                    }
                }

                // ── Location ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null,
                            tint = TextMuted, modifier = Modifier.size(18.dp))
                        BasicTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp,
                                fontFamily = MulishFamily, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(Primary),
                            decorationBox = { innerTextField ->
                                if (location.isEmpty()) {
                                    Text(text = "Location (optional)", color = TextMuted,
                                        fontSize = 14.sp, fontFamily = MulishFamily)
                                }
                                innerTextField()
                            },
                        )
                    }
                }

                // ── Add People — tap to open dedicated screen ──────────────────
                EventFormRow(
                    icon  = Icons.Filled.PersonAdd,
                    label = "Add people",
                    onClick = {
                        // No sync needed — attendees IS viewModel.draftAttendees
                        navController.navigate("add_people")
                    },
                ) {
                    if (attendees.isEmpty()) {
                        Text(
                            text = "None",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily, color = TextMuted,
                        )
                    } else {
                        val label = if (attendees.size == 1) "person" else "people"
                        Text(
                            text = "${attendees.size} $label",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                    }
                }

                                // ── Recurrence ────────────────────────────────────────────────
                EventFormRow(
                    icon = Icons.Filled.Repeat,
                    label = "Repeat",
                    onClick = { showRecurrencePicker = true },
                ) {
                    Text(
                        text = when (recurrenceType) {
                            RecurrenceType.NONE -> "Never"
                            RecurrenceType.DAILY -> "Every day"
                            RecurrenceType.WEEKLY -> "Every week"
                            RecurrenceType.MONTHLY -> "Every month"
                            RecurrenceType.YEARLY -> "Every year"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = if (recurrenceType == RecurrenceType.NONE) TextMuted else Primary,
                    )
                }

                // ── Reminder ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp)),
                ) {
                    EventFormRow(
                        icon = Icons.Filled.Notifications,
                        label = "Reminder",
                        onClick = { showReminderPicker = true },
                    ) {
                        Text(
                            text = when (reminderType) {
                                "NONE" -> "None"
                                "NOTIFICATION" -> "Notification"
                                "ALARM" -> "Full Alarm"
                                else -> "None"
                            },
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = if (reminderType == "NONE") TextMuted else Primary,
                        )
                    }

                    if (reminderType != "NONE") {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                        // Minutes before picker
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(text = "Remind me before", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, fontFamily = MulishFamily, color = TextMuted)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(5, 10, 15, 30, 60).forEach { mins ->
                                    val isSelected = reminderMinutesBefore == mins
                                    Box(
                                        modifier = Modifier.weight(1f).height(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Primary else SurfaceHigh)
                                            .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(10.dp))
                                            .clickable { reminderMinutesBefore = mins },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (mins < 60) "${mins}m" else "1h",
                                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                            fontFamily = MulishFamily,
                                            color = if (isSelected) Color.White else TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                        // ✅ Sound picker — only when ALARM type selected
                        if (reminderType == "ALARM") {
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        navController.navigate(
                                            com.tushartamrakar.ontime.navigation.Screen.AlarmSound
                                                .createRoute(reminderSound)
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Ringtone",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily, color = TextPrimary,
                                )
                                Text(
                                    text = reminderSound.replace("alarm_", "")
                                        .replace("_", " ")
                                        .split(" ")
                                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily, color = Primary,
                                )
                            }
                            // ✅ Label announcement toggle
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SurfaceHigh))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Announce Label",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = TextPrimary,
                                    )
                                    Text(
                                        text = "Reads event title aloud when alarm fires",
                                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                        fontFamily = MulishFamily, color = TextMuted,
                                    )
                                }
                                Switch(
                                    checked = announceLabelOnReminder,
                                    onCheckedChange = { announceLabelOnReminder = it },
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
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ─── Renotify Attendees Dialog (edit mode) ──────────────────────────
        if (showRenotifyDialog) {
            val newlyAdded = attendees.filter { a ->
                originalAttendees.none { o -> o.name == a.name }
            }
            RenotifyAttendeesDialog(
                newCount  = newlyAdded.size,
                onConfirm = {
                    showRenotifyDialog = false
                    scope.launch {
                        viewModel.sendInvitesToAttendees(
                            eventId  = eventId,
                            attendees = newlyAdded,
                            context  = context,
                        )
                    }
                    navController.navigateUp()
                },
                onDismiss = {
                    showRenotifyDialog = false
                    navController.navigateUp()
                },
            )
        }

        // ─── Category Picker Dialog ───────────────────────────────────────────
        if (showCategoryPicker) {
            CategoryPickerDialog(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it; showCategoryPicker = false },
                onDismiss = { showCategoryPicker = false },
            )
        }

        // ─── Recurrence Picker Dialog ─────────────────────────────────────────
        if (showRecurrencePicker) {
            OptionPickerDialog(
                title = "Repeat",
                options = listOf(
                    "Never" to RecurrenceType.NONE,
                    "Every day" to RecurrenceType.DAILY,
                    "Every week" to RecurrenceType.WEEKLY,
                    "Every month" to RecurrenceType.MONTHLY,
                    "Every year" to RecurrenceType.YEARLY,
                ),
                selectedValue = recurrenceType,
                onSelect = { recurrenceType = it; showRecurrencePicker = false },
                onDismiss = { showRecurrencePicker = false },
            )
        }

        // ─── Reminder Type Picker Dialog ──────────────────────────────────────
        if (showReminderPicker) {
            OptionPickerDialog(
                title = "Reminder",
                options = listOf(
                    "None" to "NONE",
                    "Notification" to "NOTIFICATION",
                    "Full Alarm" to "ALARM",
                ),
                selectedValue = reminderType,
                onSelect = { reminderType = it; showReminderPicker = false },
                onDismiss = { showReminderPicker = false },
            )
        }

        // ─── Start Date Picker ────────────────────────────────────────────────
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            if (endDate.isBefore(startDate)) endDate = startDate
                        }
                        showStartDatePicker = false
                    }) { Text("OK", color = Primary, fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
                        Text("Cancel", color = TextMuted, fontFamily = MulishFamily)
                    }
                },
            ) { DatePicker(state = datePickerState) }
        }

        // ─── End Date Picker ──────────────────────────────────────────────────
        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            if (!picked.isBefore(startDate)) endDate = picked
                        }
                        showEndDatePicker = false
                    }) { Text("OK", color = Primary, fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) {
                        Text("Cancel", color = TextMuted, fontFamily = MulishFamily)
                    }
                },
            ) { DatePicker(state = datePickerState) }
        }

        // ─── Start Time Picker ────────────────────────────────────────────────
        if (showStartTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = startTime.hour, initialMinute = startTime.minute, is24Hour = false
            )
            TimePickerDialog(
                onDismiss = { showStartTimePicker = false },
                onConfirm = {
                    startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    if (endTime.isBefore(startTime)) endTime = startTime.plusHours(1)
                    showStartTimePicker = false
                },
            ) { TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(clockDialColor = CardBackground, selectorColor = Primary)) }
        }

        // ─── End Time Picker ──────────────────────────────────────────────────
        if (showEndTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = endTime.hour, initialMinute = endTime.minute, is24Hour = false
            )
            TimePickerDialog(
                onDismiss = { showEndTimePicker = false },
                onConfirm = {
                    endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showEndTimePicker = false
                },
            ) { TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(clockDialColor = CardBackground, selectorColor = Primary)) }
        }
    }
}

// ─── Renotify dialog (edit mode) ─────────────────────────────────────────────
@Composable
private fun RenotifyAttendeesDialog(
    newCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Notify attendees?",
                fontSize = 17.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
            )
        },
        text = {
            val who = if (newCount == 1) "1 new person" else "$newCount new people"
            Text(
                "You added $who. Send them an invite now?",
                fontSize = 14.sp, fontFamily = MulishFamily,
                color = TextMuted, lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Yes, send invite",
                    color = Primary, fontFamily = MulishFamily,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Save quietly",
                    color = TextMuted, fontFamily = MulishFamily,
                    fontSize = 14.sp,
                )
            }
        },
    )
}

// ─── Attendee Chip ────────────────────────────────────────────────────────────
@Composable
private fun AttendeeChip(
    attendee: EventAttendeeEntity,
    onToggleSms: () -> Unit,
    onToggleEmail: () -> Unit,
    onRemove: () -> Unit,
) {
    val initials = attendee.name.split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(40.dp))
            .background(SurfaceHigh)
            .border(1.dp, Border, RoundedCornerShape(40.dp))
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = Primary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = attendee.name.split(" ").first(), // first name only for compactness
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextPrimary,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // SMS pill — only shown if contact has a phone
                if (!attendee.phone.isNullOrBlank()) {
                    AttendeeNotifPill(
                        label   = "SMS",
                        active  = attendee.notifyViaSms,
                        onClick = onToggleSms,
                    )
                }
                // Email pill — only shown if contact has an email
                if (!attendee.email.isNullOrBlank()) {
                    AttendeeNotifPill(
                        label   = "Email",
                        active  = attendee.notifyViaEmail,
                        onClick = onToggleEmail,
                    )
                }
            }
        }

        // Remove button
        Icon(
            Icons.Filled.Close,
            contentDescription = "Remove",
            tint = TextMuted,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() },
        )
    }
}

@Composable
private fun AttendeeNotifPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                1.dp,
                if (active) Primary else Border,
                RoundedCornerShape(6.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (active) "✓ $label" else label,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = if (active) Primary else TextMuted,
        )
    }
}

// ─── Event Form Row ───────────────────────────────────────────────────────────
@Composable
private fun EventFormRow(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextPrimary)
        }
        trailing()
    }
}

// ─── Category Picker Dialog ───────────────────────────────────────────────────
@Composable
private fun CategoryPickerDialog(
    categories: List<EventCategoryEntity>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CardBackground).padding(20.dp),
        ) {
            Text(text = "Choose Category", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            categories.forEach { category ->
                val isSelected = category.id == selectedId
                val color = parseColor(category.colorHex)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                        .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) color else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { onSelect(category.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
                    Text(text = "${category.emoji} ${category.name}", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                        color = if (isSelected) color else TextPrimary)
                    if (isSelected) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "✓", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            color = color, fontFamily = MulishFamily)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ─── Generic Option Picker Dialog ────────────────────────────────────────────
@Composable
private fun <T> OptionPickerDialog(
    title: String,
    options: List<Pair<String, T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CardBackground).padding(20.dp),
        ) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            options.forEach { (label, value) ->
                val isSelected = value == selectedValue
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = if (isSelected) Primary else TextPrimary)
                    if (isSelected) {
                        Text(text = "✓", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            color = Primary, fontFamily = MulishFamily)
                    }
                }
            }
        }
    }
}

// ─── Time Picker Dialog Wrapper ───────────────────────────────────────────────
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .background(CardBackground).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted, fontFamily = MulishFamily, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onConfirm) {
                    Text("OK", color = Primary, fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
