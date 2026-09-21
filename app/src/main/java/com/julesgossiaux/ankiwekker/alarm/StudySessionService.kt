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
    private val activeAlarmIds = mutableSetOf<String>()
    private var monitorJob: Job? = null
    private var alarmPlayer: MediaPlayer? = null
    private var sessionEnded = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Vérification des cartes dues…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            endSession()
            return START_NOT_STICKY
        }

        sessionEnded = false
        serviceScope.launch {
            val sessionStore = SessionStore(applicationContext)
            val persistedIds = sessionStore.readActiveAlarmIds()
            val incomingId = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID)
            val ids = when {
                intent?.action == ACTION_RESTART -> persistedIds
                incomingId != null -> persistedIds + incomingId
                persistedIds.isNotEmpty() -> persistedIds
                else -> AlarmStore(applicationContext).readAll().filter { it.enabled }.map { it.id }.toSet()
            }
            synchronized(activeAlarmIds) { activeAlarmIds.addAll(ids) }
            sessionStore.saveActiveAlarmIds(activeAlarmIdsSnapshot())
            startMonitorIfNeeded()
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stopAlarmSound()
        getSystemService(NotificationManager::class.java).cancel(AlarmReceiver.NOTIFICATION_ID)
        if (!sessionEnded) SessionWatchdog.schedule(applicationContext)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        SessionWatchdog.schedule(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitorIfNeeded() {
        if (monitorJob == null) {
            monitorJob = serviceScope.launch { monitorSessions() }
        }
    }

    private suspend fun monitorSessions() {
        val gateway = AnkiDroidGateway(applicationContext)
        val lastDueCounts = mutableMapOf<String, Int>()
        val studyDetected = mutableSetOf<String>()

        while (serviceScope.isActive) {
            val alarms = AlarmStore(applicationContext).readAll()
                .associateBy { it.id }
            val currentIds = activeAlarmIdsSnapshot().filter { it in alarms }.toSet()
            synchronized(activeAlarmIds) {
                activeAlarmIds.retainAll(currentIds)
            }
            if (currentIds.isEmpty()) {
                endSession()
                return
            }

            var shouldSound = false
            var studying = false
            var remainingSessions = 0
            for (alarmId in currentIds) {
                val alarm = alarms.getValue(alarmId)
                when (val result = gateway.readDueCards(alarm.selectedDeckIds)) {
                    is AnkiDroidResult.Success -> {
                        val currentDueCount = result.value.total
                        if (currentDueCount == 0) {
                            synchronized(activeAlarmIds) { activeAlarmIds.remove(alarmId) }
                            lastDueCounts.remove(alarmId)
                            studyDetected.remove(alarmId)
                            continue
                        }
                        remainingSessions++
                        val previous = lastDueCounts[alarmId]
                        if (previous != null && currentDueCount < previous) {
                            studyDetected += alarmId
                        }
                        if (studyDetected.contains(alarmId)) {
                            studying = true
                            if (previous == currentDueCount) shouldSound = true
                        } else {
                            shouldSound = true
                        }
                        lastDueCounts[alarmId] = currentDueCount
                    }
                    is AnkiDroidResult.Failure -> {
                        remainingSessions++
                        shouldSound = true
                    }
                }
            }

            SessionStore(applicationContext).saveActiveAlarmIds(activeAlarmIdsSnapshot())
            if (remainingSessions == 0 || activeAlarmIdsSnapshot().isEmpty()) {
                endSession()
                return
            }

            updateNotification(
                if (studying) {
                    "$remainingSessions session(s) — étude en cours"
                } else {
                    "$remainingSessions session(s) — cartes dues restantes"
                },
            )
            val pollInterval = if (studying) STUDY_POLL_MILLIS else POLL_INTERVAL_MILLIS
            val burstDuration = if (studying) STUDY_BURST_MILLIS else ALARM_BURST_MILLIS
            if (shouldSound) {
                playAlarmBurst()
                delay(burstDuration)
                stopAlarmSound()
                delay((pollInterval - burstDuration).coerceAtLeast(0))
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
        alarmPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@StudySessionService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun stopAlarmSound() {
        alarmPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
        }
        alarmPlayer = null
    }

    private fun endSession() {
        if (sessionEnded) return
        sessionEnded = true
        stopAlarmSound()
        synchronized(activeAlarmIds) { activeAlarmIds.clear() }
        SessionWatchdog.cancel(applicationContext)
        serviceScope.launch {
            SessionStore(applicationContext).saveActiveAlarmIds(emptySet())
            stopSelf()
        }
    }

    private fun activeAlarmIdsSnapshot(): Set<String> = synchronized(activeAlarmIds) { activeAlarmIds.toSet() }

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

    companion object {
        private const val ACTION_STOP = "com.julesgossiaux.ankiwekker.STOP_SESSION"
        private const val ACTION_RESTART = "com.julesgossiaux.ankiwekker.RESTART_SESSION"
        private const val SESSION_CHANNEL_ID = "anki_review_session"
        private const val NOTIFICATION_ID = 2101
        private const val ALARM_BURST_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 10_000L
        private const val STUDY_BURST_MILLIS = 5_000L
        private const val STUDY_POLL_MILLIS = 5_000L

        fun stopIntent(context: Context): Intent =
            Intent(context, StudySessionService::class.java).setAction(ACTION_STOP)

        fun restartIntent(context: Context): Intent =
            Intent(context, StudySessionService::class.java).setAction(ACTION_RESTART)
    }
}
