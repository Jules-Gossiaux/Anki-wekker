package com.julesgossiaux.ankiwekker.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.julesgossiaux.ankiwekker.MainActivity
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidResult
import com.julesgossiaux.ankiwekker.selection.DeckSelectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudySessionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var alarmPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Vérification des cartes dues…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (monitorJob == null) {
            monitorJob = serviceScope.launch { monitorSession() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopAlarmSound()
        getSystemService(NotificationManager::class.java).cancel(AlarmReceiver.NOTIFICATION_ID)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun monitorSession() {
        val gateway = AnkiDroidGateway(applicationContext)
        val selectionStore = DeckSelectionStore(applicationContext)
        var lastDueCount: Int? = null
        var studyDetected = false

        while (serviceScope.isActive) {
            val selectedDeckIds = selectionStore.readSelectedDeckIds()
            var shouldSound = true
            when (val result = gateway.readDueCards(selectedDeckIds)) {
                is AnkiDroidResult.Success -> {
                    val currentDueCount = result.value.total
                    if (currentDueCount == 0) {
                        stopSelf()
                        return
                    }
                    if (lastDueCount != null && currentDueCount < lastDueCount!!) {
                        studyDetected = true
                        shouldSound = false
                        updateNotification(
                            "Étude détectée — $currentDueCount carte(s) restante(s)",
                        )
                    } else if (studyDetected) {
                        updateNotification(
                            "Aucune nouvelle carte depuis 5 secondes — relance",
                        )
                    } else {
                        updateNotification("$currentDueCount carte(s) due(s) restante(s)")
                    }
                    lastDueCount = currentDueCount
                }
                is AnkiDroidResult.Failure -> {
                    updateNotification("AnkiDroid indisponible — nouvelle tentative")
                }
            }

            val pollInterval = if (studyDetected) STUDY_POLL_MILLIS else POLL_INTERVAL_MILLIS
            val burstDuration = if (studyDetected) STUDY_BURST_MILLIS else ALARM_BURST_MILLIS
            if (shouldSound) {
                playAlarmBurst()
                delay(burstDuration)
                stopAlarmSound()
                delay(pollInterval - burstDuration)
            } else {
                stopAlarmSound()
                delay(pollInterval)
            }
        }
    }

    private fun playAlarmBurst() {
        stopAlarmSound()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        alarmPlayer = MediaPlayer.create(this, alarmUri)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            isLooping = true
            start()
        }
    }

    private fun stopAlarmSound() {
        alarmPlayer?.stopSafely()
        alarmPlayer?.release()
        alarmPlayer = null
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1101,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1102,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, SESSION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Anki-wekker actif")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .addAction(0, "Arrêter", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                SESSION_CHANNEL_ID,
                "Session de révision Anki",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun MediaPlayer.stopSafely() {
        if (isPlaying) stop()
    }

    companion object {
        private const val ACTION_STOP = "com.julesgossiaux.ankiwekker.STOP_SESSION"
        private const val SESSION_CHANNEL_ID = "anki_review_session"
        private const val NOTIFICATION_ID = 2101
        private const val ALARM_BURST_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 20_000L
        private const val STUDY_BURST_MILLIS = 5_000L
        private const val STUDY_POLL_MILLIS = 5_000L

        fun stopIntent(context: Context): Intent =
            Intent(context, StudySessionService::class.java).setAction(ACTION_STOP)
    }
}
