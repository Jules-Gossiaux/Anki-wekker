package com.julesgossiaux.ankiwekker

import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.julesgossiaux.ankiwekker.alarm.AlarmScheduler
import com.julesgossiaux.ankiwekker.alarm.AlarmSettings
import com.julesgossiaux.ankiwekker.alarm.AlarmStore
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDeck
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidResult
import com.julesgossiaux.ankiwekker.ankidroid.DueCardsSnapshot
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnkiWekkerApp(
                gateway = AnkiDroidGateway(applicationContext),
                alarmStore = AlarmStore(applicationContext),
                alarmScheduler = AlarmScheduler(applicationContext),
            )
        }
    }
}

@Composable
private fun AnkiWekkerApp(
    gateway: AnkiDroidGateway,
    alarmStore: AlarmStore,
    alarmScheduler: AlarmScheduler,
) {
    var alarms by remember { mutableStateOf<List<AlarmSettings>>(emptyList()) }
    var editingAlarm by remember { mutableStateOf<AlarmSettings?>(null) }
    var status by remember { mutableStateOf("Prêt à configurer les alarmes") }
    var snapshot by remember { mutableStateOf<DueCardsSnapshot?>(null) }
    var decks by remember { mutableStateOf<List<AnkiDeck>>(emptyList()) }
    var dueCountsByDeckId by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showDeckSelection by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        alarms = alarmStore.readAll()
        if (alarmScheduler.canScheduleExactAlarms()) alarmScheduler.scheduleAll(alarms)
    }

    fun persistAlarms(updated: List<AlarmSettings>, message: String) {
        alarms.filter { old -> updated.none { it.id == old.id } }.forEach(alarmScheduler::cancel)
        alarms = updated
        scope.launch {
            alarmStore.saveAll(updated)
            if (alarmScheduler.canScheduleExactAlarms()) alarmScheduler.scheduleAll(updated)
            status = message
        }
    }

    fun loadDecks(alarm: AlarmSettings) {
        if (!gateway.hasDatabasePermission()) {
            status = "Autorise l'accès AnkiDroid pour sélectionner les decks"
            return
        }
        loading = true
        status = "Lecture des decks…"
        showDeckSelection = true
        scope.launch {
            when (val result = gateway.listDecks()) {
                is AnkiDroidResult.Success -> {
                    decks = result.value
                    when (val dueResult = gateway.readDueCards()) {
                        is AnkiDroidResult.Success -> {
                            dueCountsByDeckId = dueResult.value.decks.associate { it.identifier to it.cardCount }
                            status = "${decks.size} deck(s) chargé(s)"
                        }
                        is AnkiDroidResult.Failure -> {
                            dueCountsByDeckId = emptyMap()
                            status = "Decks chargés, compteur indisponible"
                        }
                    }
                    editingAlarm = alarm.copy(
                        selectedDeckIds = expandParentSelection(result.value, alarm.selectedDeckIds),
                    )
                }
                is AnkiDroidResult.Failure -> status = result.message
            }
            loading = false
        }
    }

    fun readDueCards(selectedDeckIds: Set<String>) {
        if (!gateway.hasDatabasePermission()) {
            status = "Autorise l'accès AnkiDroid pour lire les cartes"
            return
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
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Anki-wekker", style = MaterialTheme.typography.headlineMedium)
                Text("Alarmes de révision", modifier = Modifier.padding(top = 12.dp))
                Text(status, modifier = Modifier.padding(top = 16.dp))

                Button(
                    enabled = !loading,
                    onClick = {
                        editingAlarm = AlarmSettings(enabled = true)
                        showDeckSelection = false
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text("Ajouter une alarme") }

                alarms.forEach { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        nextOccurrence = alarmScheduler.nextOccurrence(alarm),
                        onEdit = {
                            editingAlarm = alarm
                            showDeckSelection = false
                        },
                        onToggle = {
                            persistAlarms(
                                alarms.map { if (it.id == alarm.id) it.copy(enabled = !it.enabled) else it },
                                if (alarm.enabled) "Alarme désactivée" else "Alarme activée",
                            )
                        },
                        onDelete = {
                            persistAlarms(alarms.filterNot { it.id == alarm.id }, "Alarme supprimée")
                        },
                    )
                }

                if (alarms.isEmpty()) {
                    Text("Aucune alarme configurée", modifier = Modifier.padding(top = 20.dp))
                }

                Button(
                    enabled = !loading,
                    onClick = { readDueCards(editingAlarm?.selectedDeckIds ?: emptySet()) },
                    modifier = Modifier.padding(top = 20.dp),
                ) { Text("Lire les cartes dues") }
                Button(
                    onClick = {
                        status = if (gateway.openAnkiDroid()) "AnkiDroid ouvert" else "Impossible d'ouvrir AnkiDroid"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Ouvrir AnkiDroid") }
                snapshot?.let { DueSummary(it) }

                editingAlarm?.let { alarm ->
                    AlarmEditor(
                        alarm = alarm,
                        decks = decks,
                        dueCountsByDeckId = dueCountsByDeckId,
                        showDeckSelection = showDeckSelection,
                        loading = loading,
                        onAlarmChange = { editingAlarm = it },
                        onLoadDecks = { loadDecks(alarm) },
                        onReadDueCards = { readDueCards(alarm.selectedDeckIds) },
                        onConfirm = {
                            if (alarm.activeDays.isEmpty()) {
                                status = "Sélectionne au moins un jour"
                            } else if (!alarmScheduler.canScheduleExactAlarms()) {
                                context.startActivity(alarmScheduler.exactAlarmSettingsIntent())
                                status = "Autorise les alarmes exactes puis réessaie"
                            } else {
                                val updated = if (alarms.any { it.id == alarm.id }) {
                                    alarms.map { if (it.id == alarm.id) alarm else it }
                                } else {
                                    alarms + alarm
                                }
                                persistAlarms(updated, "Alarme enregistrée")
                                editingAlarm = null
                                showDeckSelection = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                }
                            }
                        },
                        onCancel = {
                            editingAlarm = null
                            showDeckSelection = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmSettings,
    nextOccurrence: java.time.ZonedDateTime?,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Text(
            text = "${formatAlarmTime(alarm.hour, alarm.minute)} — ${if (alarm.enabled) "Active" else "Désactivée"}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Jours : ${formatDays(alarm.activeDays)}")
        Text("Fuseau : ${alarm.zoneId}")
        Text("Decks : ${if (alarm.selectedDeckIds.isEmpty()) "tous" else alarm.selectedDeckIds.size}")
        Text("Prochaine occurrence : ${formatOccurrence(nextOccurrence)}")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onEdit, modifier = Modifier.padding(end = 4.dp)) { Text("Modifier") }
            Button(onClick = onToggle, modifier = Modifier.padding(end = 4.dp)) {
                Text(if (alarm.enabled) "Désactiver" else "Activer")
            }
            Button(onClick = onDelete) { Text("Supprimer") }
        }
    }
}

@Composable
private fun AlarmEditor(
    alarm: AlarmSettings,
    decks: List<AnkiDeck>,
    dueCountsByDeckId: Map<String, Int>,
    showDeckSelection: Boolean,
    loading: Boolean,
    onAlarmChange: (AlarmSettings) -> Unit,
    onLoadDecks: () -> Unit,
    onReadDueCards: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text("Modifier l'alarme", style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onAlarmChange(alarm.copy(hour = hour, minute = minute)) },
                    alarm.hour,
                    alarm.minute,
                    true,
                ).show()
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(formatAlarmTime(alarm.hour, alarm.minute)) }

        Text("Jours actifs", modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            (1..7).forEach { day ->
                Button(
                    onClick = {
                        val days = if (day in alarm.activeDays) alarm.activeDays - day else alarm.activeDays + day
                        onAlarmChange(alarm.copy(activeDays = days))
                    },
                    modifier = Modifier.padding(end = 2.dp),
                ) { Text(dayLabels.getValue(day).take(2)) }
            }
        }
        Text("Fuseau : ${alarm.zoneId}")
        Button(onClick = onLoadDecks, enabled = !loading, modifier = Modifier.padding(top = 8.dp)) {
            Text("Sélectionner les decks")
        }
        Button(onClick = onReadDueCards, enabled = !loading, modifier = Modifier.padding(top = 8.dp)) {
            Text("Tester le compteur")
        }
        if (showDeckSelection && decks.isNotEmpty()) {
            Text("Aucune sélection = tous les decks", modifier = Modifier.padding(top = 12.dp))
            DeckSelectionTree(
                decks = decks,
                dueCountsByDeckId = dueCountsByDeckId,
                selectedDeckIds = alarm.selectedDeckIds,
                onSelectionChanged = { ids, checked ->
                    onAlarmChange(alarm.copy(
                        selectedDeckIds = if (checked) alarm.selectedDeckIds + ids else alarm.selectedDeckIds - ids,
                    ))
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = onConfirm, enabled = !loading, modifier = Modifier.padding(end = 8.dp)) {
                Text("Enregistrer")
            }
            Button(onClick = onCancel) { Text("Annuler") }
        }
    }
}

@Composable
private fun ColumnScope.DueSummary(snapshot: DueCardsSnapshot) {
    Text("Total dû : ${snapshot.total}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
    snapshot.decks.sortedBy { it.name }.forEach { deck ->
        Text("${deck.name} : ${deck.cardCount}", modifier = Modifier.padding(top = 2.dp))
    }
    Spacer(modifier = Modifier.padding(bottom = 4.dp))
}

private val dayLabels = mapOf(
    1 to "Lundi", 2 to "Mardi", 3 to "Mercredi", 4 to "Jeudi",
    5 to "Vendredi", 6 to "Samedi", 7 to "Dimanche",
)

private fun formatDays(days: Set<Int>): String =
    if (days.isEmpty()) "aucun" else days.sorted().joinToString(", ") { dayLabels.getValue(it).take(2) }

private fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun formatOccurrence(value: java.time.ZonedDateTime?): String = value?.format(
    DateTimeFormatter.ofPattern("EEE dd/MM HH:mm z", Locale.getDefault()),
) ?: "aucune"

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
    val roots = remember(decks, dueCountsByDeckId) { buildDeckTree(decks, dueCountsByDeckId) }
    Column(modifier = modifier) {
        roots.forEach { node ->
            DeckTreeRow(
                node = node,
                level = 0,
                expandedPaths = expandedPaths,
                onExpandToggle = { path ->
                    expandedPaths = if (path in expandedPaths) expandedPaths - path else expandedPaths + path
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
            modifier = Modifier.padding(start = (level * 20).dp).weight(0.08f),
            style = MaterialTheme.typography.titleMedium,
        )
        node.deck?.let {
            Checkbox(
                checked = nodeDeckIds.all { it in selectedDeckIds },
                onCheckedChange = { checked -> onSelectionChanged(nodeDeckIds, checked) },
            )
        }
        Text(
            text = node.name,
            fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Text(node.dueCount.toString(), fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal)
    }
    if (expanded) {
        node.children.forEach { child ->
            DeckTreeRow(child, level + 1, expandedPaths, onExpandToggle, selectedDeckIds, onSelectionChanged)
        }
    }
}

private fun DeckTreeNode.allDeckIds(): Set<String> = buildSet {
    deck?.let { add(it.identifier) }
    children.forEach { addAll(it.allDeckIds()) }
}

private fun buildDeckTree(decks: List<AnkiDeck>, dueCountsByDeckId: Map<String, Int>): List<DeckTreeNode> {
    class MutableNode(
        val name: String,
        val path: String,
        var deck: AnkiDeck? = null,
        val children: MutableMap<String, MutableNode> = sortedMapOf(),
    )
    val roots = sortedMapOf<String, MutableNode>()
    decks.forEach { deck ->
        var currentChildren: MutableMap<String, MutableNode> = roots
        var path = ""
        deck.name.split("::").forEachIndexed { index, part ->
            path = if (path.isEmpty()) part else "$path::$part"
            val node = currentChildren.getOrPut(part) { MutableNode(part, path) }
            if (index == deck.name.split("::").lastIndex) node.deck = deck
            currentChildren = node.children
        }
    }
    fun convert(nodes: Collection<MutableNode>): List<DeckTreeNode> = nodes.sortedBy { it.name }.map { node ->
        val children = convert(node.children.values)
        DeckTreeNode(
            name = node.name,
            path = node.path,
            deck = node.deck,
            dueCount = (node.deck?.let { dueCountsByDeckId[it.identifier] ?: 0 } ?: 0) + children.sumOf { it.dueCount },
            children = children,
        )
    }
    return convert(roots.values)
}

private fun expandParentSelection(decks: List<AnkiDeck>, selectedDeckIds: Set<String>): Set<String> {
    val selectedNames = decks.filter { it.identifier in selectedDeckIds }.map { it.name }
    return selectedDeckIds + decks.filter { deck ->
        selectedNames.any { parent -> deck.name == parent || deck.name.startsWith("$parent::") }
    }.map { it.identifier }
}
