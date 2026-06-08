package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CalendarEventEntity::class, EventCategoryEntity::class, EventAttendeeEntity::class],
    version = 7,
    exportSchema = false,
)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun attendeeDao(): AttendeeDao

    companion object {
        const val DATABASE_NAME = "ontime_calendar_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN reminderSound TEXT NOT NULL DEFAULT 'alarm_digital_alarm'")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN announceLabelOnReminder INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN remindersJson TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN priority TEXT NOT NULL DEFAULT 'NONE'")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // holidays table was added in v5 — kept for existing users upgrading from v4
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS holidays (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        name TEXT NOT NULL,
                        localName TEXT NOT NULL DEFAULT '',
                        countryCode TEXT NOT NULL,
                        isPublicHoliday INTEGER NOT NULL DEFAULT 1,
                        types TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop holidays table — we now use LiveHolidayCache (in-memory, API-driven)
                database.execSQL("DROP TABLE IF EXISTS holidays")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add event_attendees table for the "Add people" feature
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS event_attendees (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT,
                        email TEXT,
                        notifyViaSms INTEGER NOT NULL DEFAULT 0,
                        notifyViaEmail INTEGER NOT NULL DEFAULT 0,
                        addedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            EventCategoryEntity(id = 1, name = "Work",     colorHex = "#5C6BC0", emoji = "💼", isDefault = true),
            EventCategoryEntity(id = 2, name = "Personal", colorHex = "#26A69A", emoji = "😊", isDefault = true),
            EventCategoryEntity(id = 3, name = "Health",   colorHex = "#EF5350", emoji = "❤️", isDefault = true),
            EventCategoryEntity(id = 4, name = "Study",    colorHex = "#FFA726", emoji = "📚", isDefault = true),
            EventCategoryEntity(id = 5, name = "Other",    colorHex = "#8D6E63", emoji = "📌", isDefault = true),
        )
    }
}
