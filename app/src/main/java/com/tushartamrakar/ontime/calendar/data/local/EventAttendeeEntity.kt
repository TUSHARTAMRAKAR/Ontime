package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per attendee per event.
 *
 * Kept in a separate table (not on CalendarEventEntity) so the event
 * record stays lightweight and attendee data can be queried, inserted,
 * and deleted independently without touching the event itself.
 *
 * eventId links back to calendar_events.id — no Room FK annotation
 * so we can do manual cascade delete in the ViewModel (Room FK with
 * CASCADE would require rebuilding the parent table in a migration).
 */
@Entity(tableName = "event_attendees")
data class EventAttendeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // ─── Parent event ─────────────────────────────────────────────────────────
    val eventId: Int,                           // FK → calendar_events.id

    // ─── Contact info (from ContactsContract search) ─────────────────────────
    val name: String,                           // display name from contacts
    val phone: String? = null,                  // null if contact has no phone
    val email: String? = null,                  // null if contact has no email

    // ─── Notification preferences (per-contact, toggled by user) ─────────────
    val notifyViaSms: Boolean = false,          // send SMS invite + day-of SMS
    val notifyViaEmail: Boolean = false,        // send email invite + day-of notification

    // ─── Metadata ─────────────────────────────────────────────────────────────
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Lightweight model returned from contact search (not stored in DB).
 * Converted to EventAttendeeEntity when user adds them to the event.
 */
data class ContactResult(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val avatarUri: String? = null,             // content URI string for contact photo
)

/** Convert a search result into an attendee entity ready to be inserted. */
fun ContactResult.toAttendee(eventId: Int): EventAttendeeEntity =
    EventAttendeeEntity(
        eventId = eventId,
        name    = name,
        phone   = phone,
        email   = email,
        // Default both notifications ON if the channel is available
        notifyViaSms   = phone != null,
        notifyViaEmail = email != null,
    )
