package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendeeDao {

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Live stream of attendees for a given event.
     * Used in CreateEventScreen to populate chips in edit mode —
     * auto-updates if the list changes.
     */
    @Query("SELECT * FROM event_attendees WHERE eventId = :eventId ORDER BY addedAt ASC")
    fun getForEvent(eventId: Int): Flow<List<EventAttendeeEntity>>

    /**
     * One-shot suspend version — used by AttendeeNotificationHelper
     * to load attendees just before sending notifications, without
     * subscribing to a live stream.
     */
    @Query("SELECT * FROM event_attendees WHERE eventId = :eventId ORDER BY addedAt ASC")
    suspend fun getForEventOnce(eventId: Int): List<EventAttendeeEntity>

    /**
     * Count of attendees for an event — used by the calendar DayCell
     * to decide whether to show the people badge dot.
     * Returns 0 if no attendees. Lightweight: doesn't load full rows.
     */
    @Query("SELECT COUNT(*) FROM event_attendees WHERE eventId = :eventId")
    fun getAttendeeCount(eventId: Int): Flow<Int>

    /**
     * Batch count for ALL events — used by CalendarViewModel to build
     * the eventId → hasAttendees map for the calendar month view.
     * Returns one row per eventId that has at least one attendee.
     */
    @Query("SELECT eventId FROM event_attendees GROUP BY eventId")
    fun getEventIdsWithAttendees(): Flow<List<Int>>

    // ─── Write ────────────────────────────────────────────────────────────────

    /**
     * Insert a batch of attendees. REPLACE strategy handles re-saves
     * gracefully in case of duplicate inserts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendees: List<EventAttendeeEntity>)

    /**
     * Delete all attendees for a specific event.
     * Called in two places:
     *   1. Before re-saving attendees (delete old → insert new batch)
     *   2. When the parent event is deleted (manual cascade)
     */
    @Query("DELETE FROM event_attendees WHERE eventId = :eventId")
    suspend fun deleteAllForEvent(eventId: Int)
}
