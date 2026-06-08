package com.tushartamrakar.ontime.calendar.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.LiveHoliday
import com.tushartamrakar.ontime.core.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─── Search result types ──────────────────────────────────────────────────────
sealed class SearchResult {
    data class YearHeader(val year: Int) : SearchResult()
    data class HolidayResult(val holiday: LiveHoliday) : SearchResult()
    data class EventResult(val event: CalendarEventEntity, val category: com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity?) : SearchResult()
}

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val allHolidays by viewModel.allHolidaysForSearch.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val isLoadingHolidays by viewModel.isLoadingHolidays.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus search field on open
    // Load full holiday range for search + auto-focus
    LaunchedEffect(Unit) {
        viewModel.loadAllHolidaysForSearch()
        focusRequester.requestFocus()
    }

    // ── Smart match function ──────────────────────────────────────────────────
    // Handles: exact substring, word-level match, common festival synonyms
    fun smartMatch(text: String, q: String): Boolean {
        if (text.isBlank()) return false
        val t = text.lowercase()
        if (t.contains(q)) return true
        // Split query into words — match if ALL words appear in text
        val words = q.split(" ", "-").filter { it.length >= 3 }
        if (words.size > 1 && words.all { w -> t.contains(w) }) return true
        // Festival synonym map — covers alternate spellings
        val synonyms = mapOf(
            "diwali"    to listOf("deepavali","deepawali","dipawali","dipavali","dīpāvalī"),
            "holi"      to listOf("holika","dhuleti","dhulandi"),
            "eid"       to listOf("eid-ul-fitr","eid-ul-adha","eid al fitr","eid al adha","idul fitri"),
            "navratri"  to listOf("navaratri","navarathri","navratri"),
            "christmas" to listOf("xmas","x-mas","nativity"),
            "dussehra"  to listOf("dasehra","vijayadashami","dasara"),
            "pongal"    to listOf("makara sankranti","thai pongal"),
            "onam"      to listOf("thiruvonam"),
            "baisakhi"  to listOf("vaisakhi","vaishakhi"),
            "raksha"    to listOf("rakshabandhan","rakhi"),
            "ganesh"    to listOf("ganapati","vinayaka chaturthi","ganesh chaturthi"),
            "independence" to listOf("independence day","swatantrata diwas"),
            "republic"  to listOf("republic day","gantantra diwas"),
        )
        synonyms.forEach { (key, alts) ->
            if (q.contains(key) || key.contains(q)) {
                if (alts.any { alt -> t.contains(alt) } || t.contains(key)) return true
            }
            if (alts.any { alt -> q.contains(alt) || alt.contains(q) }) {
                if (t.contains(key) || alts.any { alt -> t.contains(alt) }) return true
            }
        }
        return false
    }

    // Build search results grouped by year
    val results: List<SearchResult> = remember(query, allEvents, allHolidays) {
        if (query.length < 2) return@remember emptyList()
        val q = query.lowercase().trim()

        // ── Match holidays — NO distinctBy on name — show ALL years ──────────
        val matchedHolidays = allHolidays
            .filter { holiday ->
                smartMatch(holiday.name, q) ||
                smartMatch(holiday.localName, q)
            }
            // Only dedupe exact same event on the same date (not same name across years!)
            .distinctBy { "${it.date}_${it.name.lowercase().trim()}" }
            .sortedBy { it.date }

        // ── Match events from Room — all fields ───────────────────────────────
        val matchedEvents = allEvents
            .filter { event ->
                smartMatch(event.title, q) ||
                smartMatch(event.description, q) ||
                smartMatch(event.location, q) ||
                // Also search by category name
                allCategories.find { it.id == event.categoryId }?.name
                    ?.lowercase()?.contains(q) == true
            }
            .sortedBy { it.startTimeMillis }

        // Merge and group by year
        val grouped = mutableListOf<SearchResult>()
        var currentYear = -1

        // Combine all items with their dates for sorting
        data class Item(val date: LocalDate, val result: SearchResult)
        val all = mutableListOf<Item>()

        matchedHolidays.forEach { holiday ->
            val date = runCatching { LocalDate.parse(holiday.date) }.getOrNull() ?: return@forEach
            all.add(Item(date, SearchResult.HolidayResult(holiday)))
        }
        matchedEvents.forEach { event ->
            val date = Instant.ofEpochMilli(event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val category = allCategories.find { it.id == event.categoryId }
            all.add(Item(date, SearchResult.EventResult(event, category)))
        }

        all.sortBy { it.date }

        all.forEach { item ->
            if (item.date.year != currentYear) {
                grouped.add(SearchResult.YearHeader(item.date.year))
                currentYear = item.date.year
            }
            grouped.add(item.result)
        }
        grouped
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        // ─── Search Bar Header ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Back arrow
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Search field
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Search, contentDescription = null,
                        tint = Primary, modifier = Modifier.size(16.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search events, holidays, festivals...",
                                color = TextMuted, fontSize = 14.sp,
                                fontFamily = MulishFamily,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = TextPrimary, fontSize = 14.sp,
                                fontFamily = MulishFamily, fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(Primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close, contentDescription = "Clear",
                                tint = TextMuted, modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // ─── Divider ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceHigh))

        // ─── Content ──────────────────────────────────────────────────────────
        when {
            query.length < 2 -> {
                // Empty state — show suggestions
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🔍", fontSize = 52.sp)
                        Text(
                            "Search for anything",
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = TextPrimary,
                        )
                        Text(
                            "Events, festivals, holidays...",
                            fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Quick search chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Diwali", "Christmas", "Holi", "Eid").forEach { suggestion ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceHigh)
                                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                                        .clickable { query = suggestion }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = MulishFamily, color = TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Still loading holidays and no results yet — show spinner instead of "no results"
            results.isEmpty() && allHolidays.isEmpty() && query.length >= 2 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Primary,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            "Searching across 10 years...",
                            fontSize = 13.sp,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("😕", fontSize = 48.sp)
                        Text(
                            "No results for \"$query\"",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily, color = TextPrimary,
                        )
                        Text(
                            "Try a different keyword",
                            fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted,
                        )
                        if (allHolidays.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Searched ${allHolidays.size} holidays across 10 years",
                                fontSize = 11.sp, fontFamily = MulishFamily,
                                color = TextMuted.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    // Result count header
                    item {
                        val holidayCount = results.count { it is SearchResult.HolidayResult }
                        val eventCount   = results.count { it is SearchResult.EventResult }
                        val parts = buildList {
                            if (holidayCount > 0) add("$holidayCount holiday${if (holidayCount > 1) "s" else ""}")
                            if (eventCount > 0)   add("$eventCount event${if (eventCount > 1) "s" else ""}")
                        }
                        Text(
                            text = parts.joinToString(" · "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = Primary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(results, key = { result ->
                        when (result) {
                            is SearchResult.YearHeader -> "year_${result.year}"
                            is SearchResult.HolidayResult -> "h_${result.holiday.date}_${result.holiday.name}"
                            is SearchResult.EventResult -> "e_${result.event.id}"
                        }
                    }) { result ->
                        when (result) {
                            is SearchResult.YearHeader -> YearHeaderRow(result.year)
                            is SearchResult.HolidayResult -> SearchHolidayRow(
                                holiday = result.holiday,
                            )
                            is SearchResult.EventResult -> SearchEventRow(
                                event = result.event,
                                category = result.category,
                                onClick = {
                                    navController.navigate("event_detail/${result.event.id}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Year Header ──────────────────────────────────────────────────────────────
@Composable
private fun YearHeaderRow(year: Int) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = year.toString(),
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily, color = TextMuted,
            letterSpacing = 1.sp,
        )
    }
}

// ─── Search Holiday Row ───────────────────────────────────────────────────────
@Composable
private fun SearchHolidayRow(holiday: LiveHoliday) {
    val date = runCatching { LocalDate.parse(holiday.date) }.getOrNull()
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("d")
    val monthFormatter = DateTimeFormatter.ofPattern("MMM")
    val holidayColor = Color(0xFFFF6B35)

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ─── Date column ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (date != null) {
                Text(
                    text = date.format(dayFormatter).uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = TextMuted,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = date.format(dateFormatter),
                    fontSize = 22.sp, fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily, color = TextPrimary,
                )
                Text(
                    text = date.format(monthFormatter).uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextMuted,
                )
            }
        }

        // ─── Dot ─────────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(holidayColor))

        // ─── Holiday info ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(holidayColor.copy(alpha = 0.1f))
                .border(1.dp, holidayColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🎉", fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holiday.name,
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = holidayColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (holiday.localName.isNotBlank() && holiday.localName != holiday.name) {
                    Text(
                        text = holiday.localName,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily, color = holidayColor.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text = "Public Holiday",
                    fontSize = 10.sp, fontFamily = MulishFamily,
                    color = holidayColor.copy(alpha = 0.5f),
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(start = 76.dp).height(0.5.dp).background(SurfaceHigh))
}

// ─── Search Event Row ─────────────────────────────────────────────────────────
@Composable
private fun SearchEventRow(
    event: CalendarEventEntity,
    category: com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity?,
    onClick: () -> Unit,
) {
    val date = Instant.ofEpochMilli(event.startTimeMillis)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val startTime = Instant.ofEpochMilli(event.startTimeMillis)
        .atZone(ZoneId.systemDefault()).toLocalTime()
    val colorHex = category?.colorHex ?: "#5C6BC0"
    val eventColor = parseColor(colorHex)
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ─── Date column ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.format(dayFormatter).uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextMuted,
            )
        }

        // ─── Dot ─────────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(eventColor))

        // ─── Event card ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(start = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color bar
            Box(
                modifier = Modifier.width(4.dp).height(52.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(eventColor),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = event.title,
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily, color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (category != null) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(eventColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${category.emoji} ${category.name}",
                                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily, color = eventColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (event.isAllDay) "All day" else startTime.format(timeFormatter),
                    fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted,
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(start = 76.dp).height(0.5.dp).background(SurfaceHigh))
}
