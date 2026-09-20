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
import androidx.compose.foundation.clickable
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
    var dueCountsByDeckId by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedDeckIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeckSelection by remember { mutableStateOf(false) }
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
        showDeckSelection = true
        snapshot = null
        scope.launch {
            val savedSelection = selectionStore.readSelectedDeckIds()
            when (val result = gateway.listDecks()) {
                is AnkiDroidResult.Success -> {
                    decks = result.value
                    selectedDeckIds = expandParentSelection(result.value, savedSelection)
                    when (val dueResult = gateway.readDueCards()) {
                        is AnkiDroidResult.Success -> {
                            dueCountsByDeckId = dueResult.value.decks.associate {
                                it.identifier to it.cardCount
                            }
                            status = "${decks.size} deck(s) chargé(s) — ${dueResult.value.total} carte(s) due(s)"
                        }
                        is AnkiDroidResult.Failure -> {
                            dueCountsByDeckId = emptyMap()
                            status = "${decks.size} deck(s) chargé(s), compteur indisponible"
                        }
                    }
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
                            val selectionToRead = if (showDeckSelection) {
                                selectedDeckIds
                            } else {
                                selectionStore.readSelectedDeckIds()
                            }
                            selectedDeckIds = selectionToRead
                            when (val result = gateway.readDueCards(selectionToRead)) {
                                is AnkiDroidResult.Success -> {
                                    snapshot = result.value
                                    dueCountsByDeckId = result.value.decks.associate {
                                        it.identifier to it.cardCount
                                    }
                                    showDeckSelection = false
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
                    Text("Sélectionner les decks")
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
                if (showDeckSelection && decks.isNotEmpty()) {
                    Text(
                        text = "Sélection des decks (aucune sélection = tous)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    DeckSelectionTree(
                        decks = decks,
                        dueCountsByDeckId = dueCountsByDeckId,
                        selectedDeckIds = selectedDeckIds,
                        onSelectionChanged = { deckIds, checked ->
                            selectedDeckIds = if (checked) {
                                selectedDeckIds + deckIds
                            } else {
                                selectedDeckIds - deckIds
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    val selectedDueTotal = if (selectedDeckIds.isEmpty()) {
                        dueCountsByDeckId.values.sum()
                    } else {
                        selectedDeckIds.sumOf { dueCountsByDeckId[it] ?: 0 }
                    }
                    Text(
                        text = "Cartes dues sélectionnées : $selectedDueTotal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Button(
                        enabled = !loading,
                        onClick = {
                            loading = true
                            status = "Enregistrement de la sélection…"
                            scope.launch {
                                selectionStore.saveSelectedDeckIds(selectedDeckIds)
                                showDeckSelection = false
                                status = "Sélection confirmée"
                                loading = false
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Confirmer la sélection")
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

private data class DeckTreeNode(
    val name: String,
    val path: String,
    val deck: AnkiDeck? = null,
    val dueCount: Int = 0,
    val children: List<DeckTreeNode> = emptyList(),
)

@Composable
private fun DeckSelectionTree(
    decks: List<AnkiDeck>,
    dueCountsByDeckId: Map<String, Int>,
    selectedDeckIds: Set<String>,
    onSelectionChanged: (Set<String>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedPaths by remember { mutableStateOf(emptySet<String>()) }
    val roots = remember(decks, dueCountsByDeckId) {
        buildDeckTree(decks, dueCountsByDeckId)
    }

    Column(modifier = modifier) {
        roots.forEach { node ->
            DeckTreeRow(
                node = node,
                level = 0,
                expandedPaths = expandedPaths,
                onExpandToggle = { path ->
                    expandedPaths = if (path in expandedPaths) {
                        expandedPaths - path
                    } else {
                        expandedPaths + path
                    }
                },
                selectedDeckIds = selectedDeckIds,
                onSelectionChanged = onSelectionChanged,
            )
        }
    }
}

@Composable
private fun DeckTreeRow(
    node: DeckTreeNode,
    level: Int,
    expandedPaths: Set<String>,
    onExpandToggle: (String) -> Unit,
    selectedDeckIds: Set<String>,
    onSelectionChanged: (Set<String>, Boolean) -> Unit,
) {
    val hasChildren = node.children.isNotEmpty()
    val expanded = node.path in expandedPaths
    val nodeDeckIds = node.allDeckIds()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasChildren) { onExpandToggle(node.path) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                !hasChildren -> ""
                expanded -> "⌄"
                else -> "›"
            },
            modifier = Modifier
                .padding(start = (level * 20).dp)
                .fillMaxWidth(0.08f),
            style = MaterialTheme.typography.titleMedium,
        )
        node.deck?.let {
            Checkbox(
                checked = nodeDeckIds.all { it in selectedDeckIds },
                onCheckedChange = { checked ->
                    onSelectionChanged(nodeDeckIds, checked)
                },
            )
        }
        Text(
            text = node.name,
            fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        Text(
            text = node.dueCount.toString(),
            fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(end = 8.dp),
        )
    }

    if (expanded) {
        node.children.forEach { child ->
            DeckTreeRow(
                node = child,
                level = level + 1,
                expandedPaths = expandedPaths,
                onExpandToggle = onExpandToggle,
                selectedDeckIds = selectedDeckIds,
                onSelectionChanged = onSelectionChanged,
            )
        }
    }
}

private fun DeckTreeNode.allDeckIds(): Set<String> = buildSet {
    deck?.let { add(it.identifier) }
    children.forEach { addAll(it.allDeckIds()) }
}

private fun buildDeckTree(
    decks: List<AnkiDeck>,
    dueCountsByDeckId: Map<String, Int>,
): List<DeckTreeNode> {
    class MutableNode(
        val name: String,
        val path: String,
        var deck: AnkiDeck? = null,
        val children: MutableMap<String, MutableNode> = sortedMapOf(),
    )

    val roots = sortedMapOf<String, MutableNode>()
    decks.forEach { deck ->
        var currentChildren: MutableMap<String, MutableNode> = roots
        val parts = deck.name.split("::")
        var path = ""
        parts.forEachIndexed { index, part ->
            path = if (path.isEmpty()) part else "$path::$part"
            val node = currentChildren.getOrPut(part) { MutableNode(part, path) }
            if (index == parts.lastIndex) node.deck = deck
            currentChildren = node.children
        }
    }

    fun convert(nodes: Collection<MutableNode>): List<DeckTreeNode> = nodes
        .sortedBy { it.name }
        .map { node ->
        val children = convert(node.children.values)
        val ownDueCount = node.deck?.let { dueCountsByDeckId[it.identifier] ?: 0 } ?: 0
        DeckTreeNode(
            name = node.name,
            path = node.path,
            deck = node.deck,
            dueCount = ownDueCount + children.sumOf { it.dueCount },
            children = children,
        )
    }

    return convert(roots.values)
}

private fun expandParentSelection(
    decks: List<AnkiDeck>,
    selectedDeckIds: Set<String>,
): Set<String> {
    val selectedNames = decks
        .filter { it.identifier in selectedDeckIds }
        .map { it.name }

    return selectedDeckIds + decks
        .filter { deck ->
            selectedNames.any { parent ->
                deck.name == parent || deck.name.startsWith("$parent::")
            }
        }
        .map { it.identifier }
}
