package com.julesgossiaux.ankiwekker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDeck
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidResult
import com.julesgossiaux.ankiwekker.ankidroid.DueCardsSnapshot
import com.julesgossiaux.ankiwekker.selection.DeckSelectionStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnkiWekkerApp(
                gateway = AnkiDroidGateway(applicationContext),
                selectionStore = DeckSelectionStore(applicationContext),
            )
        }
    }
}

@Composable
private fun AnkiWekkerApp(
    gateway: AnkiDroidGateway,
    selectionStore: DeckSelectionStore,
) {
    var status by remember { mutableStateOf("Prêt à vérifier AnkiDroid") }
    var snapshot by remember { mutableStateOf<DueCardsSnapshot?>(null) }
    var decks by remember { mutableStateOf<List<AnkiDeck>>(emptyList()) }
    var selectedDeckIds by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    fun requestOrLoadDecks() {
        if (!gateway.hasDatabasePermission()) {
            permissionLauncher.launch(AnkiDroidGateway.READ_WRITE_PERMISSION)
            return
        }
        loading = true
        status = "Lecture des decks…"
        scope.launch {
            selectedDeckIds = selectionStore.readSelectedDeckIds()
            when (val result = gateway.listDecks()) {
                is AnkiDroidResult.Success -> {
                    decks = result.value
                    status = "${decks.size} deck(s) chargé(s)"
                }
                is AnkiDroidResult.Failure -> status = result.message
            }
            loading = false
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top,
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
                            when (val result = gateway.readDueCards(selectedDeckIds)) {
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
                    enabled = !loading,
                    onClick = ::requestOrLoadDecks,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Charger les decks")
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
                if (decks.isNotEmpty()) {
                    Text(
                        text = "Decks surveillés (aucune sélection = tous)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(top = 8.dp),
                    ) {
                        items(decks, key = { it.identifier }) { deck ->
                            DeckSelectionRow(
                                deck = deck,
                                checked = deck.identifier in selectedDeckIds,
                                onCheckedChange = { checked ->
                                    selectedDeckIds = if (checked) {
                                        selectedDeckIds + deck.identifier
                                    } else {
                                        selectedDeckIds - deck.identifier
                                    }
                                    scope.launch {
                                        selectionStore.saveSelectedDeckIds(selectedDeckIds)
                                    }
                                },
                            )
                        }
                    }
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

    val groupedDecks = snapshot.decks
        .flatMap { deck ->
            val parts = deck.name.split("::")
            (1..parts.size).map { depth ->
                parts.take(depth).joinToString("::") to deck.cardCount
            }
        }
        .groupingBy { it.first }
        .fold(0) { total, entry -> total + entry.second }
        .toSortedMap()

    groupedDecks.forEach { (name, count) ->
        val level = name.count { it == ':' } / 2
        Text(
            text = "$name : $count",
            fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(
                start = (level * 20).dp,
                top = 2.dp,
            ),
        )
    }
}

@Composable
private fun DeckSelectionRow(
    deck: AnkiDeck,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(deck.name, modifier = Modifier.padding(start = 8.dp))
    }
}
