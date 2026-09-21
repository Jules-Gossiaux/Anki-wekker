package com.julesgossiaux.ankiwekker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.julesgossiaux.ankiwekker.MainActivity
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, StudySessionService::class.java),
        )
        context.packageManager.getLaunchIntentForPackage(AnkiDroidGateway.PACKAGE_NAME)?.let { ankiIntent ->
            runCatching {
                context.startActivity(
                    ankiIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
                )
            }
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Révisions Anki",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alarmes de révision AnkiDroid"
                enableVibration(true)
                setSound(null, null)
            },
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ankiIntent = context.packageManager.getLaunchIntentForPackage(AnkiDroidGateway.PACKAGE_NAME)
        val reviewIntent = ankiIntent?.let {
            PendingIntent.getActivity(
                context,
                1003,
                it.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } ?: contentIntent
        val alarmActivityIntent = PendingIntent.getActivity(
            context,
            1004,
            Intent(context, AlarmActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fullScreenIntent = if (ankiIntent != null) reviewIntent else alarmActivityIntent

        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Anki-wekker")
                .setContentText("Tes cartes Anki dues t'attendent")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenIntent, true)
                .setSilent(true)
                .setAutoCancel(false)
                .setContentIntent(reviewIntent)
                .addAction(0, "Ouvrir AnkiDroid", reviewIntent)
                .build(),
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val settings = AlarmStore(context).read()
            if (settings.enabled) {
                AlarmScheduler(context).scheduleDaily(settings.hour, settings.minute)
            }
            pendingResult.finish()
        }
    }

    companion object {
        private const val CHANNEL_ID = "anki_review_alarm_v3"
        const val NOTIFICATION_ID = 2001
    }
}
