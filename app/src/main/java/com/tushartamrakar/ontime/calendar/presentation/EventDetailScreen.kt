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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventDetailScreen(
    navController: NavHostController,
    eventId: Int,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val scope = rememberCoroutineScope()

    var event by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load event
    LaunchedEffect(eventId, allEvents) {
        event = allEvents.find { it.id == eventId }
    }

    val currentEvent = event

    if (currentEvent == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Event not found",
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextMuted,
            )
        }
        return
    }

    val category = allCategories.find { it.id == currentEvent.categoryId }
    val categoryColor = category?.let { parseColor(it.colorHex) } ?: Primary

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val startDateTime = Instant.ofEpochMilli(currentEvent.startTimeMillis)
        .atZone(ZoneId.systemDefault())
    val endDateTime = Instant.ofEpochMilli(currentEvent.endTimeMillis)
        .atZone(ZoneId.systemDefault())

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                        tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Event Details",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextPrimary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                // Edit button
                IconButton(onClick = {
                    navController.navigate(Screen.EditEvent.createRoute(currentEvent.id))
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit",
                        tint = Primary, modifier = Modifier.size(20.dp))
                }
                // Delete button
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = Danger, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // ── Title Card ────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp)),
                ) {
                    // Color bar at top
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(categoryColor),
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentEvent.title,
                            fontSize = 22.sp, fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily, color = TextPrimary,
                            letterSpacing = (-0.3).sp,
                        )
                        if (category != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                                    .background(categoryColor))
                                Text(
                                    text = "${category.emoji} ${category.name}",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily, color = categoryColor,
                                )
                            }
                        }
                    }
                }

                // ── Date & Time ───────────────────────────────────────────────
                DetailCard {
                    if (currentEvent.isAllDay) {
                        DetailRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Date",
                            value = startDateTime.toLocalDate().format(dateFormatter),
                            valueColor = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(text = "All Day", fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily, color = Primary)
                        }
                    } else {
                        DetailRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Date",
                            value = startDateTime.toLocalDate().format(dateFormatter),
                            valueColor = TextPrimary,
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp)
                            .background(SurfaceHigh).padding(vertical = 6.dp))
                        DetailRow(
                            icon = Icons.Filled.AccessTime,
                            label = "Time",
                            value = "${startDateTime.toLocalTime().format(timeFormatter)} → ${endDateTime.toLocalTime().format(timeFormatter)}",
                            valueColor = Primary,
                        )
                    }
                }

                // ── Description ───────────────────────────────────────────────
                if (currentEvent.description.isNotBlank()) {
                    DetailCard {
                        DetailRow(
                            icon = Icons.Filled.Description,
                            label = "Description",
                            value = currentEvent.description,
                            valueColor = TextPrimary,
                        )
                    }
                }

                // ── Location ──────────────────────────────────────────────────
                if (currentEvent.location.isNotBlank()) {
                    DetailCard {
                        DetailRow(
                            icon = Icons.Filled.LocationOn,
                            label = "Location",
                            value = currentEvent.location,
                            valueColor = TextPrimary,
                        )
                    }
                }

                // ── Recurrence ────────────────────────────────────────────────
                if (currentEvent.recurrenceType != "NONE") {
                    DetailCard {
                        DetailRow(
                            icon = Icons.Filled.Repeat,
                            label = "Repeats",
                            value = when (currentEvent.recurrenceType) {
                                "DAILY" -> "Every day"
                                "WEEKLY" -> "Every week"
                                "MONTHLY" -> "Every month"
                                "YEARLY" -> "Every year"
                                else -> "Never"
                            },
                            valueColor = Primary,
                        )
                    }
                }

                // ── Reminder ──────────────────────────────────────────────────
                if (currentEvent.reminderType != "NONE") {
                    DetailCard {
                        DetailRow(
                            icon = Icons.Filled.Notifications,
                            label = "Reminder",
                            value = "${currentEvent.reminderMinutesBefore} min before · ${
                                when (currentEvent.reminderType) {
                                    "NOTIFICATION" -> "Notification"
                                    "ALARM" -> "Full Alarm"
                                    else -> ""
                                }
                            }",
                            valueColor = Primary,
                        )
                    }
                }

                // ── Sync Status ───────────────────────────────────────────────
                DetailCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Google Calendar", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = TextPrimary)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (currentEvent.isSynced) Primary.copy(alpha = 0.15f) else SurfaceHigh)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (currentEvent.isSynced) "✓ Synced" else "Local only",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = if (currentEvent.isSynced) Primary else TextMuted,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ─── Delete Confirmation Dialog ───────────────────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = CardBackground,
                title = {
                    Text(text = "Delete Event?", fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = TextPrimary)
                },
                text = {
                    Text(
                        text = "\"${currentEvent.title}\" will be permanently deleted.",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily, color = TextMuted,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.deleteEvent(currentEvent)
                            showDeleteDialog = false
                            navController.navigateUp()
                        }
                    }) {
                        Text(text = "Delete", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(text = "Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily, color = TextMuted)
                    }
                },
            )
        }
    }
}

// ─── Detail Card ──────────────────────────────────────────────────────────────
@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) { content() }
}

// ─── Detail Row ───────────────────────────────────────────────────────────────
@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = MulishFamily, color = TextMuted, letterSpacing = 0.3.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = valueColor)
        }
    }
}
