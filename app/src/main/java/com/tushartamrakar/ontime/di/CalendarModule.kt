package com.tushartamrakar.ontime.di

import android.content.Context
import androidx.room.Room
import com.tushartamrakar.ontime.calendar.data.local.AttendeeDao
import com.tushartamrakar.ontime.calendar.data.local.CalendarDatabase
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventDao
import com.tushartamrakar.ontime.calendar.data.local.LiveHolidayCache
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    @Provides
    @Singleton
    fun provideCalendarDatabase(@ApplicationContext context: Context): CalendarDatabase =
        Room.databaseBuilder(context, CalendarDatabase::class.java, CalendarDatabase.DATABASE_NAME)
            .addMigrations(
                CalendarDatabase.MIGRATION_1_2,
                CalendarDatabase.MIGRATION_2_3,
                CalendarDatabase.MIGRATION_3_4,
                CalendarDatabase.MIGRATION_4_5,
                CalendarDatabase.MIGRATION_5_6, // ✅ drops holidays table
                CalendarDatabase.MIGRATION_6_7, // ✅ adds event_attendees table
            ).build()

    @Provides @Singleton
    fun provideCalendarEventDao(db: CalendarDatabase): CalendarEventDao = db.calendarEventDao()

    @Provides @Singleton
    fun provideCalendarRepository(dao: CalendarEventDao): CalendarRepository = CalendarRepository(dao)

    @Provides @Singleton
    fun provideAttendeeDao(db: CalendarDatabase): AttendeeDao = db.attendeeDao()

    @Provides @Singleton
    fun provideLiveHolidayCache(@ApplicationContext context: Context): LiveHolidayCache =
        LiveHolidayCache(context)
}
