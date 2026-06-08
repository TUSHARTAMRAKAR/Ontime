package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    // ─── Events ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Int): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTimeMillis >= :startMillis 
        AND startTimeMillis < :endMillis 
        ORDER BY startTimeMillis ASC
    """)
    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEventEntity>>

    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTimeMillis >= :dayStartMillis 
        AND startTimeMillis < :dayEndMillis 
        ORDER BY startTimeMillis ASC
    """)
    fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<CalendarEventEntity>>

    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTimeMillis > :nowMillis 
        AND reminderType != 'NONE'
        ORDER BY startTimeMillis ASC
    """)
    suspend fun getUpcomingEventsWithReminders(nowMillis: Long): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE isSynced = 0")
    suspend fun getUnsyncedEvents(): List<CalendarEventEntity>

    @Query("UPDATE calendar_events SET googleEventId = :googleId, isSynced = 1 WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, googleId: String)

    // ✅ NEW: get all Google event IDs to avoid duplicates during pull
    @Query("SELECT googleEventId FROM calendar_events WHERE googleEventId IS NOT NULL")
    suspend fun getAllGoogleEventIds(): List<String>

    // ─── Full-text search across title, description, location ─────────────────
    // Uses SQLite LIKE for case-insensitive partial match on all text fields.
    // Returns ALL matching events regardless of date (past + future).
    @Query("""
        SELECT * FROM calendar_events
        WHERE lower(title)       LIKE '%' || lower(:query) || '%'
        OR    lower(description) LIKE '%' || lower(:query) || '%'
        OR    lower(location)    LIKE '%' || lower(:query) || '%'
        ORDER BY startTimeMillis ASC
    """)
    fun searchEvents(query: String): Flow<List<CalendarEventEntity>>

    @Query("""
        SELECT * FROM calendar_events
        WHERE lower(title)       LIKE '%' || lower(:query) || '%'
        OR    lower(description) LIKE '%' || lower(:query) || '%'
        OR    lower(location)    LIKE '%' || lower(:query) || '%'
        ORDER BY startTimeMillis ASC
    """)
    suspend fun searchEventsOnce(query: String): List<CalendarEventEntity>

    // ─── Categories ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: EventCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: EventCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: EventCategoryEntity)

    @Query("SELECT * FROM event_categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<EventCategoryEntity>>

    @Query("SELECT * FROM event_categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): EventCategoryEntity?
}