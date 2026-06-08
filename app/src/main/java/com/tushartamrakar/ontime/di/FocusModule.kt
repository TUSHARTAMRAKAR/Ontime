package com.tushartamrakar.ontime.di

import android.content.Context
import androidx.room.Room
import com.tushartamrakar.ontime.focus.data.local.FocusDao
import com.tushartamrakar.ontime.focus.data.local.FocusDatabase
import com.tushartamrakar.ontime.focus.data.repository.FocusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for the Focus section.
 *
 * Completely separate from AppModule — FocusDatabase is its own SQLite file
 * ("ontime_focus.db") with no connection to Alarm, Task, or Period databases.
 * This follows the same pattern as PeriodDatabase in AppModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object FocusModule {

    @Provides
    @Singleton
    fun provideFocusDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(
            context,
            FocusDatabase::class.java,
            FocusDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFocusDao(db: FocusDatabase): FocusDao = db.focusDao()

    @Provides
    @Singleton
    fun provideFocusRepository(dao: FocusDao): FocusRepository = FocusRepository(dao)
}
