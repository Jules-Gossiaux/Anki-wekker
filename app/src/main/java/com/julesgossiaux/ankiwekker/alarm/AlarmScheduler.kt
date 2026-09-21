package com.julesgossiaux.ankiwekker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.time.Clock
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun scheduleAll(alarms: List<AlarmSettings>, clock: Clock = Clock.systemUTC()) {
        if (!canScheduleExactAlarms()) return
        alarmManager.cancel(legacyPendingIntent())
        alarms.forEach { alarm ->
            if (alarm.enabled) scheduleNext(alarm, clock) else cancel(alarm)
        }
    }

    fun scheduleNext(alarm: AlarmSettings, clock: Clock = Clock.systemUTC()) {
        if (!canScheduleExactAlarms() || !alarm.enabled) return
        nextOccurrence(alarm, clock)?.let { occurrence ->
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                occurrence.toInstant().toEpochMilli(),
                pendingIntent(alarm.id),
            )
        }
    }

    fun cancel(alarm: AlarmSettings) {
        alarmManager.cancel(pendingIntent(alarm.id))
    }

    fun nextOccurrence(alarm: AlarmSettings, clock: Clock = Clock.systemUTC()): ZonedDateTime? {
        return AlarmOccurrence.next(alarm, clock)
    }

    private fun pendingIntent(alarmId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("ankiwekker://alarm/$alarmId"))
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun legacyPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        LEGACY_REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val LEGACY_REQUEST_CODE = 1001
    }
}
