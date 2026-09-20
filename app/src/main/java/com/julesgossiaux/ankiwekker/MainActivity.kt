package com.julesgossiaux.ankiwekker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidResult
import com.julesgossiaux.ankiwekker.ankidroid.DueCardsSnapshot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnkiWekkerApp(AnkiDroidGateway(applicationContext))
        }
    }
}

@Composable
private fun AnkiWekkerApp(gateway: AnkiDroidGateway) {
    var status by remember { mutableStateOf("Prêt à vérifier AnkiDroid") }
    var snapshot by remember { mutableStateOf<DueCardsSnapshot?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) {
            "Accès AnkiDroid accordé — clique à nouveau pour lire les cartes"
        } else {
            "Accès AnkiDroid refusé — autorise la permission pour continuer"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Anki-wekker", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Diagnostic AnkiDroid",
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(status, modifier = Modifier.padding(top = 24.dp))
                Button(
                    enabled = !loading,
                    onClick = {
                        if (!gateway.hasDatabasePermission()) {
                            permissionLauncher.launch(AnkiDroidGateway.READ_WRITE_PERMISSION)
                            return@Button
                        }
                        loading = true
                        status = "Lecture des cartes dues…"
                        scope.launch {
                            when (val result = gateway.readDueCards()) {
                                is AnkiDroidResult.Success -> {
                                    snapshot = result.value
                                    status = "Lecture réussie"
                                }
                                is AnkiDroidResult.Failure -> {
                                    snapshot = null
                                    status = result.message
                                }
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Lire les cartes dues")
                }
                Button(
                    onClick = {
                        status = if (gateway.openAnkiDroid()) {
                            "AnkiDroid ouvert"
                        } else {
                            "Impossible d'ouvrir AnkiDroid"
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Ouvrir AnkiDroid")
                }
                snapshot?.let { DueSummary(it) }
            }
        }
    }
}

@Composable
private fun ColumnScope.DueSummary(snapshot: DueCardsSnapshot) {
    Text(
        text = "Total dû : ${snapshot.total}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp),
    )
    snapshot.decks.forEach { deck ->
        Text("${deck.identifier} : ${deck.cardCount}")
    }
}
