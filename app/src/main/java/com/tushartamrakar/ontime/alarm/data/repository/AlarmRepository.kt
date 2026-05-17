package com.tushartamrakar.ontime.alarm.data.repository

import com.tushartamrakar.ontime.alarm.data.local.AlarmDao
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
) {
    // ─── Get all alarms as Flow ───────────────────────────────────────────────
    fun getAllAlarms(): Flow<List<AlarmEntity>> {
        return alarmDao.getAllAlarms()
    }

    // ─── Get single alarm ─────────────────────────────────────────────────────
    suspend fun getAlarmById(id: Int): AlarmEntity? {
        return alarmDao.getAlarmById(id)
    }

    // ─── Create alarm ─────────────────────────────────────────────────────────
    suspend fun createAlarm(alarm: AlarmEntity): Long {
        val id = alarmDao.insertAlarm(alarm)
        val savedAlarm = alarm.copy(id = id.toInt())
        if (savedAlarm.isEnabled) {
            alarmScheduler.schedule(savedAlarm)
        }
        return id
    }

    // ─── Update alarm ─────────────────────────────────────────────────────────
    suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
        alarmScheduler.cancel(alarm.id)
        if (alarm.isEnabled) {
            alarmScheduler.schedule(alarm)
        }
    }

    // ─── Toggle alarm on/off ──────────────────────────────────────────────────
    suspend fun toggleAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        alarmDao.updateAlarm(updated)
        if (updated.isEnabled) {
            alarmScheduler.schedule(updated)
        } else {
            alarmScheduler.cancel(alarm.id)
        }
    }

    // ─── Delete alarm ─────────────────────────────────────────────────────────
    suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmScheduler.cancel(alarm.id)
        alarmDao.deleteAlarm(alarm)
    }
}