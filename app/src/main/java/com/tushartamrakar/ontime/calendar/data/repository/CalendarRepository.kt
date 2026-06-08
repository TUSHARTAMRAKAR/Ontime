package com.tushartamrakar.ontime.calendar.data.repository

import com.tushartamrakar.ontime.calendar.data.local.CalendarDatabase
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventDao
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalendarRepository @Inject constructor(
    private val dao: CalendarEventDao,
) {
    // ─── Events ───────────────────────────────────────────────────────────────
    fun getAllEvents(): Flow<List<CalendarEventEntity>> = dao.getAllEvents()

    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEventEntity>> =
        dao.getEventsInRange(startMillis, endMillis)

    fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEventEntity>> =
        dao.getEventsForDay(dayStartMillis, dayEndMillis)

    suspend fun getEventById(id: Int): CalendarEventEntity? = dao.getEventById(id)

    suspend fun createEvent(event: CalendarEventEntity): Long = dao.insertEvent(event)

    suspend fun updateEvent(event: CalendarEventEntity) = dao.updateEvent(event)

    suspend fun deleteEvent(event: CalendarEventEntity) = dao.deleteEvent(event)

    suspend fun getUpcomingEventsWithReminders(nowMillis: Long) =
        dao.getUpcomingEventsWithReminders(nowMillis)

    suspend fun getUnsyncedEvents() = dao.getUnsyncedEvents()

    suspend fun markAsSynced(localId: Int, googleId: String) =
        dao.markAsSynced(localId, googleId)

    // ✅ NEW: for Google sync deduplication
    suspend fun getAllGoogleEventIds(): List<String> = dao.getAllGoogleEventIds()

    // ─── Categories ───────────────────────────────────────────────────────────
    fun getAllCategories(): Flow<List<EventCategoryEntity>> = dao.getAllCategories()

    suspend fun getCategoryById(id: Int): EventCategoryEntity? = dao.getCategoryById(id)

    suspend fun createCategory(category: EventCategoryEntity): Long =
        dao.insertCategory(category)

    suspend fun updateCategory(category: EventCategoryEntity) = dao.updateCategory(category)

    suspend fun deleteCategory(category: EventCategoryEntity) = dao.deleteCategory(category)

    suspend fun seedDefaultCategories() {
        CalendarDatabase.DEFAULT_CATEGORIES.forEach { dao.insertCategory(it) }
    }
}