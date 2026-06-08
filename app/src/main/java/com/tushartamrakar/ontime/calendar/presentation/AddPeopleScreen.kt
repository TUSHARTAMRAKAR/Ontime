package com.tushartamrakar.ontime.calendar.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.calendar.data.local.ContactResult
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import com.tushartamrakar.ontime.calendar.data.local.toAttendee
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPeopleScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val scope          = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // ── Working list — starts from whatever CreateEventScreen put in draftAttendees ──
    val attendees = viewModel.draftAttendees   // live reference — mutations reflected instantly

    var query          by remember { mutableStateOf("") }
    var searchResults  by remember { mutableStateOf<List<ContactResult>>(emptyList()) }
    var suggestions    by remember { mutableStateOf<List<ContactResult>>(emptyList()) }
    var isSearching    by remember { mutableStateOf(false) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* graceful — search just returns empty if denied */ }

    // ── Load suggestions once on open ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
        suggestions = viewModel.getSuggestedContacts()
    }

    // ── Live search with 300 ms debounce ─────────────────────────────────────
    LaunchedEffect(query) {
        if (query.length < 2) {
            searchResults = emptyList()
            isSearching   = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        searchResults = viewModel.searchContacts(query)
        isSearching   = false
    }

    // ── Detect if the typed text is a valid email or phone ───────────────────
    val isValidEmail = remember(query) {
        query.contains("@") && query.contains(".") && query.length >= 5 &&
            !query.contains(" ") && query.indexOf("@") > 0
    }
    val isValidPhone = remember(query) {
        val digits = query.filter { it.isDigit() }
        digits.length in 7..15 && query.all { it.isDigit() || it in "+-() " }
    }
    val canAddManually = (isValidEmail || isValidPhone) &&
        attendees.none { a ->
            (isValidEmail && a.email == query.trim()) ||
            (isValidPhone && a.phone == query.trim())
        }

    // ── Helper — add contact as attendee ─────────────────────────────────────
    fun addAttendee(result: ContactResult) {
        if (attendees.size >= 10) return
        if (attendees.any { a -> a.name == result.name }) return
        attendees.add(result.toAttendee(eventId = 0))
        query = ""
    }

    // ── Helper — add the currently typed text as a manual attendee ───────────
    fun addManualEntry() {
        if (attendees.size >= 10) return
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val attendee = when {
            isValidEmail -> EventAttendeeEntity(
                eventId = 0,
                name = trimmed,                  // use email as display name
                email = trimmed,
                notifyViaEmail = true,
                notifyViaSms = false,
            )
            isValidPhone -> EventAttendeeEntity(
                eventId = 0,
                name = trimmed,                  // use phone as display name
                phone = trimmed,
                notifyViaSms = true,
                notifyViaEmail = false,
            )
            else -> return
        }
        if (attendees.none { it.email == attendee.email && it.phone == attendee.phone }) {
            attendees.add(attendee)
        }
        query = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "Add people",
                fontSize = 18.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            // "Done" — auto-saves the typed entry if it's a valid email/phone
            TextButton(onClick = {
                if (canAddManually) addManualEntry()
                navController.navigateUp()
            }) {
                val finalCount = attendees.size + (if (canAddManually) 1 else 0)
                Text(
                    text = if (finalCount == 0) "Done"
                           else "Done  ($finalCount)",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = Primary,
                )
            }
        }

        // ── Search field (pill shape, like Google Calendar) ───────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceHigh)
                .border(1.dp, if (query.isNotEmpty()) Primary else Border, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length == 1) contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = TextPrimary, fontSize = 15.sp,
                    fontFamily = MulishFamily,
                ),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Add people or groups",
                            color = TextMuted, fontSize = 15.sp,
                            fontFamily = MulishFamily,
                        )
                    }
                    inner()
                },
            )
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Icon(
                    Icons.Filled.Close, contentDescription = "Clear",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { query = "" },
                )
            }
        }

        // ── Selected chips (FlowRow — wraps to next line if many) ─────────────
        AnimatedVisibility(visible = attendees.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
            ) {
                attendees.toList().forEach { attendee ->
                    SelectedPersonChip(
                        attendee = attendee,
                        onRemove = { attendees.remove(attendee) },
                    )
                }
            }
        }

        HorizontalDivider(
            color = Border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 4.dp),
        )

        // ── Body: suggestions or search results ───────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            if (query.length < 2) {
                // ── Suggestions header + list ─────────────────────────────────
                item {
                    Text(
                        text = "Suggestions",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = TextMuted,
                        modifier = Modifier.padding(
                            start = 20.dp, top = 18.dp, bottom = 6.dp,
                        ),
                    )
                }
                items(suggestions, key = { c: ContactResult -> c.name + (c.email ?: c.phone ?: "") }) { contact ->
                    val alreadyAdded = attendees.any { a -> a.name == contact.name }
                    PersonRow(
                        contact      = contact,
                        alreadyAdded = alreadyAdded,
                        onClick      = { addAttendee(contact) },
                    )
                }
                if (suggestions.isEmpty()) {
                    item {
                        Text(
                            text = "Start typing to search your contacts",
                            fontSize = 14.sp, fontFamily = MulishFamily,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        )
                    }
                }
            } else {
                // ── "Add [email] as guest" — when typed text is valid email/phone ──
                if (canAddManually) {
                    item {
                        ManualAddRow(
                            input    = query.trim(),
                            isEmail  = isValidEmail,
                            onClick  = { addManualEntry() },
                        )
                    }
                }

                // ── Search results ─────────────────────────────────────────────
                if (isSearching) {
                    item {
                        Text(
                            text = "Searching...",
                            fontSize = 13.sp, fontFamily = MulishFamily,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                } else if (searchResults.isEmpty()) {
                    // Only show "no contacts" hint when query isn't a valid email/phone
                    // (otherwise the manual add row above is the action they need)
                    if (!canAddManually) {
                        item {
                            Text(
                                text = "No contacts found for \"$query\"",
                                fontSize = 13.sp, fontFamily = MulishFamily,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    }
                } else {
                    items(searchResults, key = { c: ContactResult -> c.name + (c.email ?: c.phone ?: "") }) { contact ->
                        val alreadyAdded = attendees.any { a -> a.name == contact.name }
                        PersonRow(
                            contact      = contact,
                            alreadyAdded = alreadyAdded,
                            onClick      = { addAttendee(contact) },
                        )
                    }
                }
            }
        }
    }
}

// ─── Person row ───────────────────────────────────────────────────────────────
@Composable
private fun PersonRow(
    contact: ContactResult,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
) {
    val initials = contact.name
        .split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (alreadyAdded) SurfaceHigh
                    else Primary.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = if (alreadyAdded) TextMuted else Primary,
            )
        }

        // Name + detail
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = MulishFamily,
                color = if (alreadyAdded) TextMuted else TextPrimary,
                maxLines = 1,
            )
            val detail = contact.email ?: contact.phone
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    fontSize = 12.sp, fontFamily = MulishFamily,
                    color = TextMuted, maxLines = 1,
                )
            }
        }

        // Added checkmark or add hint
        if (alreadyAdded) {
            Text(
                text = "✓",
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                color = Primary, fontFamily = MulishFamily,
            )
        }
    }
}

// ─── Manual add row — "Add [email] as guest" ────────────────────────────────
// Shown when user types a valid email or phone that's not in their contacts.
// One tap → adds them as an external attendee with notify flag set per channel.
@Composable
private fun ManualAddRow(
    input: String,
    isEmail: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // "+" badge instead of avatar initials — signals this is a new add
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f))
                .border(1.dp, Primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = Primary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Add as guest",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = Primary,
                maxLines = 1,
            )
            Text(
                text = input,
                fontSize = 12.sp, fontFamily = MulishFamily,
                color = TextMuted, maxLines = 1,
            )
        }

        // Channel hint (email or SMS)
        Text(
            text = if (isEmail) "📧" else "💬",
            fontSize = 16.sp,
        )
    }
}

// ─── Selected person chip ─────────────────────────────────────────────────────
@Composable
private fun SelectedPersonChip(
    attendee: EventAttendeeEntity,
    onRemove: () -> Unit,
) {
    val initials = attendee.name
        .split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(40.dp))
            .background(Primary.copy(alpha = 0.12f))
            .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(40.dp))
            .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = Primary,
            )
        }
        Text(
            text = attendee.name.split(" ").first(),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily, color = Primary,
        )
        Icon(
            Icons.Filled.Close, contentDescription = "Remove",
            tint = Primary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(13.dp)
                .clickable { onRemove() },
        )
    }
}
