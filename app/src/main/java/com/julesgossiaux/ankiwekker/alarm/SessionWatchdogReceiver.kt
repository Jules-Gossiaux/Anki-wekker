package com.julesgossiaux.ankiwekker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            if (SessionStore(context).isActive()) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, StudySessionService::class.java),
                )
            }
            pendingResult.finish()
        }
    }
}
