package com.tushartamrakar.ontime.calendar.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped store holding the current draft attendee list while the
 * user is creating or editing an event.
 *
 * WHY THIS EXISTS
 * ────────────────
 * `CreateEventScreen` and `AddPeopleScreen` each get their own
 * NavBackStackEntry-scoped `CalendarViewModel` instance — so storing draft
 * state directly on the ViewModel would NOT sync between the two screens.
 * Adding an attendee in `AddPeopleScreen` would mutate one ViewModel's list
 * while `CreateEventScreen` continued reading from a different list, and the
 * user's selection would silently disappear on return.
 *
 * By scoping the list to `@Singleton`, both ViewModel instances inject the
 * SAME `SnapshotStateList`. Adds in either screen are immediately observable
 * by the other through Compose's snapshot system.
 *
 * Lifecycle: cleared on first composition of `CreateEventScreen` so a previous
 * event's draft never leaks into a new one (handled via a `rememberSaveable`
 * `initialized` flag in the screen itself).
 */
@Singleton
class AttendeeDraftStore @Inject constructor() {
    val draftAttendees: SnapshotStateList<EventAttendeeEntity> = mutableStateListOf()
}
