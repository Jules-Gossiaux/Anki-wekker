package com.julesgossiaux.ankiwekker.alarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway

class AlarmActivity : ComponentActivity() {
    private var alarmPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        startAlarmSound()
        openAnkiDroid()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Révision AnkiDroid", style = MaterialTheme.typography.headlineMedium)
                    Text("Tes cartes dues t'attendent")
                    Button(onClick = { finish() }) {
                        Text("Arrêter la sonnerie")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        alarmPlayer?.release()
        alarmPlayer = null
        super.onDestroy()
    }

    private fun startAlarmSound() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: Settings.System.DEFAULT_NOTIFICATION_URI
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

    private fun openAnkiDroid() {
        packageManager.getLaunchIntentForPackage(AnkiDroidGateway.PACKAGE_NAME)?.let { intent ->
            startActivity(intent)
        }
    }
}
