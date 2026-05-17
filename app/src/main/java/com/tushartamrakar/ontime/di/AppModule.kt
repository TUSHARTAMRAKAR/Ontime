package com.tushartamrakar.ontime.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.tushartamrakar.ontime.alarm.data.local.AlarmDao
import com.tushartamrakar.ontime.alarm.data.local.AlarmDatabase
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Room Database ────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideAlarmDatabase(
        @ApplicationContext context: Context,
    ): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME,
        ).build()
    }

    // ─── Alarm DAO ────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }

    // ─── Alarm Scheduler ──────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
    ): AlarmScheduler {
        return AlarmScheduler(context)
    }

    // ─── Firebase Auth ────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}