package com.tushartamrakar.ontime.calendar.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.Date

class GoogleCalendarSyncManager(
    private val context: Context,
    private val repository: CalendarRepository,
) {
    private fun getService(account: GoogleSignInAccount): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Arrays.asList(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_EVENTS),
        ).apply {
            backOff = ExponentialBackOff()
            selectedAccount = account.account
        }
        return Calendar.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential,
        ).setApplicationName("Ontime").build()
    }

    // ─── Pull personal events from Google ────────────────────────────────────
    suspend fun pullFromGoogle(account: GoogleSignInAccount): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val service = getService(account)
                val now = com.google.api.client.util.DateTime(Date())
                val events = service.events().list("primary")
                    .setMaxResults(250)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                var count = 0
                val existingGoogleIds = repository.getAllGoogleEventIds()

                events.items?.forEach { gEvent ->
                    val gId = gEvent.id ?: return@forEach
                    if (existingGoogleIds.contains(gId)) return@forEach

                    val startMillis = gEvent.start?.dateTime?.value
                        ?: gEvent.start?.date?.value ?: return@forEach
                    val endMillis = gEvent.end?.dateTime?.value
                        ?: gEvent.end?.date?.value ?: startMillis

                    val entity = CalendarEventEntity(
                        title = gEvent.summary ?: "Untitled",
                        description = gEvent.description ?: "",
                        location = gEvent.location ?: "",
                        startTimeMillis = startMillis,
                        endTimeMillis = endMillis,
                        isAllDay = gEvent.start?.date != null,
                        googleEventId = gId,
                        isSynced = true,
                    )
                    repository.createEvent(entity)
                    count++
                }
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Push personal events to Google ──────────────────────────────────────
    suspend fun pushToGoogle(account: GoogleSignInAccount): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val service = getService(account)
                val unsyncedEvents = repository.getUnsyncedEvents()
                var count = 0

                unsyncedEvents.forEach { entity ->
                    try {
                        val gEvent = Event().apply {
                            summary = entity.title
                            description = entity.description
                            location = entity.location
                            if (entity.isAllDay) {
                                val date = com.google.api.client.util.DateTime(true, entity.startTimeMillis, null)
                                start = EventDateTime().setDate(date)
                                end = EventDateTime().setDate(
                                    com.google.api.client.util.DateTime(true, entity.endTimeMillis, null)
                                )
                            } else {
                                start = EventDateTime().setDateTime(
                                    com.google.api.client.util.DateTime(entity.startTimeMillis)
                                )
                                end = EventDateTime().setDateTime(
                                    com.google.api.client.util.DateTime(entity.endTimeMillis)
                                )
                            }
                        }
                        val created = service.events().insert("primary", gEvent).execute()
                        repository.markAsSynced(entity.id, created.id)
                        count++
                    } catch (e: Exception) { /* skip failed */ }
                }
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Full two-way sync (personal events only) ─────────────────────────────
    // Holidays are now loaded LIVE from Google Calendar API via LiveHolidayCache
    suspend fun fullSync(account: GoogleSignInAccount): SyncResult {
        val pulled = pullFromGoogle(account)
        val pushed = pushToGoogle(account)
        return SyncResult(
            pulledCount = pulled.getOrDefault(0),
            pushedCount = pushed.getOrDefault(0),
            pullError = pulled.exceptionOrNull()?.message,
            pushError = pushed.exceptionOrNull()?.message,
        )
    }

    data class SyncResult(
        val pulledCount: Int,
        val pushedCount: Int,
        val pullError: String?,
        val pushError: String?,
    ) {
        val isSuccess get() = pullError == null && pushError == null
        val summary get() = "↓ $pulledCount pulled  ·  ↑ $pushedCount pushed"
    }
}
