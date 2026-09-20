package com.julesgossiaux.ankiwekker.alarm

import android.content.Intent
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        startSessionService()
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
                    Button(onClick = {
                        stopService(StudySessionService.stopIntent(this@AlarmActivity))
                        finish()
                    }) {
                        Text("Arrêter la sonnerie")
                    }
                }
            }
        }
    }

    private fun startSessionService() {
        androidx.core.content.ContextCompat.startForegroundService(
            this,
            Intent(this, StudySessionService::class.java),
        )
    }

    private fun openAnkiDroid() {
        packageManager.getLaunchIntentForPackage(AnkiDroidGateway.PACKAGE_NAME)?.let { intent ->
            startActivity(intent)
        }
    }
}
