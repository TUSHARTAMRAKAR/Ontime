package com.tushartamrakar.ontime.period.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    // One-shot suspend version — used for batch computation without subscribing to a stream
    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    suspend fun getAllCyclesOnce(): List<CycleEntity>

    @Query("SELECT * FROM cycles ORDER BY startDate DESC LIMIT :limit")
    fun getRecentCycles(limit: Int = 6): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestCycle(): CycleEntity?

    @Query("SELECT COUNT(*) FROM cycles")
    fun getCycleCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: CycleEntity): Long

    @Update
    suspend fun update(cycle: CycleEntity)

    @Delete
    suspend fun delete(cycle: CycleEntity)

    @Query("DELETE FROM cycles")
    suspend fun deleteAll()
}

@Dao
interface PeriodLogDao {
    @Query("SELECT * FROM period_daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<PeriodDailyLog>>

    @Query("SELECT * FROM period_daily_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun getLogsInRange(from: Long, to: Long): Flow<List<PeriodDailyLog>>

    // One-shot suspend version — used by getPhaseMapForRange() for batch loading
    @Query("SELECT * FROM period_daily_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getLogsInRangeOnce(from: Long, to: Long): List<PeriodDailyLog>

    @Query("SELECT * FROM period_daily_logs WHERE date = :date LIMIT 1")
    suspend fun getLogForDate(date: Long): PeriodDailyLog?

    @Query("SELECT * FROM period_daily_logs WHERE cycleId = :cycleId ORDER BY date ASC")
    fun getLogsForCycle(cycleId: Int): Flow<List<PeriodDailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PeriodDailyLog): Long

    @Update
    suspend fun update(log: PeriodDailyLog)

    @Delete
    suspend fun delete(log: PeriodDailyLog)

    @Query("DELETE FROM period_daily_logs")
    suspend fun deleteAll()
}

@Dao
interface PeriodSettingsDao {
    @Query("SELECT * FROM period_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<PeriodSettings?>

    @Query("SELECT * FROM period_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): PeriodSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: PeriodSettings)

    @Query("DELETE FROM period_settings")
    suspend fun deleteAll()
}
