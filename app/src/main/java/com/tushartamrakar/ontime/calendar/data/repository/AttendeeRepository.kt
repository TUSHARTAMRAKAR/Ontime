package com.tushartamrakar.ontime.calendar.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.tushartamrakar.ontime.calendar.data.local.AttendeeDao
import com.tushartamrakar.ontime.calendar.data.local.ContactResult
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendeeRepository @Inject constructor(
    private val dao: AttendeeDao,
    @ApplicationContext private val context: Context,
) {

    // ─── DB operations ────────────────────────────────────────────────────────

    /** Live list of attendees — used in CreateEventScreen to populate chips in edit mode. */
    fun getForEvent(eventId: Int): Flow<List<EventAttendeeEntity>> =
        dao.getForEvent(eventId)

    /** One-shot list — used just before sending notifications. */
    suspend fun getForEventOnce(eventId: Int): List<EventAttendeeEntity> =
        dao.getForEventOnce(eventId)

    /**
     * Atomic replace — delete all existing attendees for this event then
     * insert the new list in one shot. Safe to call on both create and update.
     */
    suspend fun saveAttendees(eventId: Int, attendees: List<EventAttendeeEntity>) {
        dao.deleteAllForEvent(eventId)
        if (attendees.isNotEmpty()) {
            dao.insertAll(attendees.map { it.copy(eventId = eventId) })
        }
    }

    /** Called when the parent event is deleted — prevents orphaned rows. */
    suspend fun deleteAllForEvent(eventId: Int) = dao.deleteAllForEvent(eventId)

    /** Live stream of event IDs that have at least one attendee. */
    fun getEventIdsWithAttendees(): Flow<List<Int>> = dao.getEventIdsWithAttendees()

    // ─── Contact search ───────────────────────────────────────────────────────

    /**
     * Searches the device contact book by name or email.
     *
     * Strategy:
     *   1. Query Email URI → get contactId, name, email, avatar
     *   2. Query Phone URI with same name filter → get phones
     *   3. Merge by contactId so each contact appears once with both phone + email
     *
     * Returns up to 10 results sorted by display name.
     * Requires READ_CONTACTS permission — caller must check before invoking.
     * Returns empty list (not a crash) if permission is absent.
     */
    suspend fun searchContacts(query: String): List<ContactResult> {
        if (query.length < 2) return emptyList()

        return withContext(Dispatchers.IO) {
            // contactId → mutable result (we'll fill phone + email from two queries)
            val merged = mutableMapOf<Long, MutableContactResult>()

            // ── Step 1: query emails ──────────────────────────────────────────
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ),
                    // match name OR email
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?" +
                    " OR ${ContactsContract.CommonDataKinds.Email.ADDRESS} LIKE ?",
                    arrayOf("%$query%", "%$query%"),
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 20",
                )?.use { cursor ->
                    val idCol    = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val nameCol  = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val emailCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(idCol)
                        val name      = cursor.getString(nameCol)?.trim() ?: continue
                        val email     = cursor.getString(emailCol)?.trim() ?: continue
                        if (name.isBlank() || email.isBlank()) continue

                        val existing = merged[contactId]
                        if (existing != null) {
                            // Keep the first email found (primary) — don't overwrite
                            if (existing.email == null) existing.email = email
                        } else {
                            merged[contactId] = MutableContactResult(
                                name      = name,
                                email     = email,
                                avatarUri = cursor.getString(photoCol),
                            )
                        }
                    }
                }
            }

            // ── Step 2: query phones for matching contacts ─────────────────────
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ),
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
                    arrayOf("%$query%"),
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 20",
                )?.use { cursor ->
                    val idCol    = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol  = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val phoneCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(idCol)
                        val name      = cursor.getString(nameCol)?.trim() ?: continue
                        val phone     = cursor.getString(phoneCol)?.trim() ?: continue
                        if (name.isBlank() || phone.isBlank()) continue

                        val existing = merged[contactId]
                        if (existing != null) {
                            // Add phone to an already-found email contact
                            if (existing.phone == null) existing.phone = phone
                        } else {
                            // Phone-only contact (no email found in step 1)
                            merged[contactId] = MutableContactResult(
                                name      = name,
                                phone     = phone,
                                avatarUri = cursor.getString(photoCol),
                            )
                        }
                    }
                }
            }

            // ── Step 3: convert to ContactResult, filter out useless rows ─────
            merged.values
                .filter { it.email != null || it.phone != null } // must have at least one
                .sortedBy  { it.name }
                .take(10)
                .map { it.toContactResult() }
        }
    }

    /**
     * Returns ~8 suggested contacts sorted by TIMES_CONTACTED (most frequent first).
     * Shown in AddPeopleScreen when the search field is empty — same as Google Calendar.
     * Falls back to alphabetical order if TIMES_CONTACTED is unavailable.
     */
    suspend fun getSuggestedContacts(): List<ContactResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<Long, MutableContactResult>()

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Contacts.TIMES_CONTACTED,
                ),
                null, null,
                "${ContactsContract.Contacts.TIMES_CONTACTED} DESC",
            )?.use { cursor ->
                val idCol    = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val nameCol  = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val emailCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext() && results.size < 8) {
                    val contactId = cursor.getLong(idCol)
                    val name  = cursor.getString(nameCol)?.trim() ?: continue
                    val email = cursor.getString(emailCol)?.trim() ?: continue
                    if (name.isBlank() || email.isBlank()) continue
                    if (!results.containsKey(contactId)) {
                        results[contactId] = MutableContactResult(
                            name      = name,
                            email     = email,
                            avatarUri = cursor.getString(photoCol),
                        )
                    }
                }
            }
        }

        // Fill phones for found contacts
        if (results.isNotEmpty()) {
            val ids = results.keys.joinToString(",")
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($ids)",
                    null, null,
                )?.use { cursor ->
                    val idCol    = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val phoneCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val cid   = cursor.getLong(idCol)
                        val phone = cursor.getString(phoneCol)?.trim() ?: continue
                        results[cid]?.let { if (it.phone == null) it.phone = phone }
                    }
                }
            }
        }

        results.values.take(8).map { it.toContactResult() }
    }

    // ─── Internal mutable builder for merge step ──────────────────────────────

    private data class MutableContactResult(
        val name: String,
        var email: String?     = null,
        var phone: String?     = null,
        val avatarUri: String? = null,
    ) {
        fun toContactResult() = ContactResult(
            name      = name,
            email     = email,
            phone     = phone,
            avatarUri = avatarUri,
        )
    }
}
