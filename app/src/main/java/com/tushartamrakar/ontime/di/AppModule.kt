package com.tushartamrakar.ontime.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tushartamrakar.ontime.alarm.data.local.AlarmDao
import com.tushartamrakar.ontime.alarm.data.local.AlarmDatabase
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import com.tushartamrakar.ontime.tasks.data.local.TaskDao
import com.tushartamrakar.ontime.tasks.data.local.TaskDatabase
import com.tushartamrakar.ontime.tasks.data.local.TaskListDao
import com.tushartamrakar.ontime.tasks.data.repository.TaskRepository
import com.tushartamrakar.ontime.period.data.local.PeriodDatabase
import com.tushartamrakar.ontime.period.data.local.CycleDao
import com.tushartamrakar.ontime.period.data.local.PeriodLogDao
import com.tushartamrakar.ontime.period.data.local.PeriodSettingsDao
import com.tushartamrakar.ontime.period.data.repository.PeriodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Alarm Database (untouched — v8) ──────────────────────────────────────
    @Provides
    @Singleton
    fun provideAlarmDatabase(@ApplicationContext context: Context): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AlarmDatabase.MIGRATION_1_2,
                AlarmDatabase.MIGRATION_2_3,
                AlarmDatabase.MIGRATION_3_4,
                AlarmDatabase.MIGRATION_4_5,
                AlarmDatabase.MIGRATION_5_6,
                AlarmDatabase.MIGRATION_6_7,
                AlarmDatabase.MIGRATION_7_8,
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao = database.alarmDao()

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler =
        AlarmScheduler(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ─── Task Database (v1 — completely separate from AlarmDatabase) ──────────
    // AlarmDatabase stays at v8 with its own migrations. TaskDatabase starts
    // fresh at v1. Zero risk of breaking existing alarm data.
    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "ontime_tasks.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideTaskListDao(database: TaskDatabase): TaskListDao = database.taskListDao()

    // ─── Period Database (v1 — separate, private, for cycle tracking) ────────
    @Provides
    @Singleton
    fun providePeriodDatabase(@ApplicationContext context: Context): PeriodDatabase {
        return Room.databaseBuilder(
            context,
            PeriodDatabase::class.java,
            "ontime_period.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides @Singleton
    fun provideCycleDao(db: PeriodDatabase): CycleDao = db.cycleDao()

    @Provides @Singleton
    fun providePeriodLogDao(db: PeriodDatabase): PeriodLogDao = db.periodLogDao()

    @Provides @Singleton
    fun providePeriodSettingsDao(db: PeriodDatabase): PeriodSettingsDao = db.periodSettingsDao()

    @Provides @Singleton
    fun providePeriodRepository(
        cycleDao: CycleDao,
        logDao: PeriodLogDao,
        settingsDao: PeriodSettingsDao,
    ): PeriodRepository = PeriodRepository(cycleDao, logDao, settingsDao)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        taskListDao: TaskListDao,
    ): TaskRepository = TaskRepository(taskDao, taskListDao)
}
