package com.julesgossiaux.ankiwekker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.julesgossiaux.ankiwekker.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Révisions Anki",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alarmes de révision AnkiDroid"
                enableVibration(true)
            },
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ankiIntent = context.packageManager.getLaunchIntentForPackage("com.ichi2.anki")
        val reviewIntent = ankiIntent?.let {
            PendingIntent.getActivity(
                context,
                1003,
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } ?: contentIntent

        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Anki-wekker")
                .setContentText("Tes cartes Anki dues t'attendent")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
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
        private const val CHANNEL_ID = "anki_review_alarm"
        private const val NOTIFICATION_ID = 2001
    }
}
