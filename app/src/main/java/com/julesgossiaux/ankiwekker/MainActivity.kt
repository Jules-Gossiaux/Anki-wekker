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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.time.ZonedDateTime
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
    var editingAlarmId by remember { mutableStateOf<String?>(null) }
    var draftAlarm by remember { mutableStateOf<AlarmSettings?>(null) }
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

    fun closeEditor() {
        editingAlarmId = null
        draftAlarm = null
        showDeckSelection = false
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

    fun openEditor(alarm: AlarmSettings) {
        editingAlarmId = alarm.id
        draftAlarm = alarm
        showDeckSelection = false
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
                    draftAlarm = alarm.copy(
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

    fun saveDraft(alarm: AlarmSettings) {
        when {
            alarm.activeDays.isEmpty() -> status = "Sélectionne au moins un jour actif"
            !alarmScheduler.canScheduleExactAlarms() -> {
                context.startActivity(alarmScheduler.exactAlarmSettingsIntent())
                status = "Autorise les alarmes exactes puis réessaie"
            }
            else -> {
                val updated = if (alarms.any { it.id == alarm.id }) {
                    alarms.map { if (it.id == alarm.id) alarm else it }
                } else {
                    alarms + alarm
                }
                persistAlarms(updated, "Alarme enregistrée")
                closeEditor()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                }
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "Anki-wekker",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Ton réveil d'étude, configuré autour de tes cartes dues.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                StatusCard(status = status, modifier = Modifier.padding(top = 20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Mes alarmes", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${alarms.size} configurée(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            val newAlarm = AlarmSettings(enabled = true)
                            editingAlarmId = newAlarm.id
                            draftAlarm = newAlarm
                            showDeckSelection = false
                        },
                    ) { Text("+ Ajouter") }
                }

                if (alarms.isEmpty()) {
                    EmptyAlarmsCard(modifier = Modifier.padding(top = 12.dp))
                }

                alarms.forEach { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        nextOccurrence = alarmScheduler.nextOccurrence(alarm),
                        onEdit = { openEditor(alarm) },
                        onToggle = {
                            persistAlarms(
                                alarms.map { if (it.id == alarm.id) it.copy(enabled = !it.enabled) else it },
                                if (alarm.enabled) "Alarme désactivée" else "Alarme activée",
                            )
                        },
                        onDelete = {
                            if (editingAlarmId == alarm.id) closeEditor()
                            persistAlarms(alarms.filterNot { it.id == alarm.id }, "Alarme supprimée")
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (editingAlarmId == alarm.id) {
                        draftAlarm?.let { draft ->
                            AlarmEditor(
                                alarm = draft,
                                decks = decks,
                                dueCountsByDeckId = dueCountsByDeckId,
                                showDeckSelection = showDeckSelection,
                                loading = loading,
                                onAlarmChange = { draftAlarm = it },
                                onLoadDecks = { loadDecks(draft) },
                                onReadDueCards = { readDueCards(draft.selectedDeckIds) },
                                onConfirm = { saveDraft(draft) },
                                onCancel = ::closeEditor,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                if (draftAlarm != null && alarms.none { it.id == editingAlarmId }) {
                    AlarmEditor(
                        alarm = draftAlarm!!,
                        decks = decks,
                        dueCountsByDeckId = dueCountsByDeckId,
                        showDeckSelection = showDeckSelection,
                        loading = loading,
                        onAlarmChange = { draftAlarm = it },
                        onLoadDecks = { loadDecks(draftAlarm!!) },
                        onReadDueCards = { readDueCards(draftAlarm!!.selectedDeckIds) },
                        onConfirm = { saveDraft(draftAlarm!!) },
                        onCancel = ::closeEditor,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                DiagnosticSection(
                    loading = loading,
                    onReadDueCards = { readDueCards(draftAlarm?.selectedDeckIds ?: emptySet()) },
                    onOpenAnkiDroid = {
                        status = if (gateway.openAnkiDroid()) "AnkiDroid ouvert" else "Impossible d'ouvrir AnkiDroid"
                    },
                    snapshot = snapshot,
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("État", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(status, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun EmptyAlarmsCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Aucune alarme pour le moment", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ajoute une alarme pour commencer tes révisions au réveil.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmSettings,
    nextOccurrence: ZonedDateTime?,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatAlarmTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (alarm.enabled) "Active" else "Désactivée",
                        color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            AlarmSummaryLine("Jours", formatDays(alarm.activeDays))
            AlarmSummaryLine("Decks", if (alarm.selectedDeckIds.isEmpty()) "Tous les decks" else "${alarm.selectedDeckIds.size} sélectionné(s)")
            AlarmSummaryLine("Prochaine", formatOccurrence(nextOccurrence))
            Text(
                "Fuseau : ${alarm.zoneId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Modifier") }
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Supprimer") }
            }
        }
    }
}

@Composable
private fun AlarmSummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(0.35f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(0.65f), fontWeight = FontWeight.Medium)
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Modifier l'alarme", style = MaterialTheme.typography.titleLarge)
            Text("Les changements seront appliqués à cette alarme uniquement.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))

            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onAlarmChange(alarm.copy(hour = hour, minute = minute)) },
                        alarm.hour,
                        alarm.minute,
                        true,
                    ).show()
                },
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("Heure : ${formatAlarmTime(alarm.hour, alarm.minute)}") }

            Text("Jours actifs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Appuie sur les jours souhaités. Les jours sélectionnés sont colorés.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            DaySelector(
                selectedDays = alarm.activeDays,
                onDayToggle = { day ->
                    onAlarmChange(alarm.copy(activeDays = toggleDay(alarm.activeDays, day)))
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            if (alarm.activeDays.isEmpty()) {
                Text("Sélectionne au moins un jour pour enregistrer.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            }

            Text("Decks AnkiDroid", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                if (alarm.selectedDeckIds.isEmpty()) "Tous les decks seront surveillés." else "${alarm.selectedDeckIds.size} deck(s) sélectionné(s).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onLoadDecks, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Choisir les decks") }
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(onClick = onReadDueCards, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Tester") }
            }
            if (showDeckSelection && decks.isNotEmpty()) {
                DeckSelectionTree(
                    decks = decks,
                    dueCountsByDeckId = dueCountsByDeckId,
                    selectedDeckIds = alarm.selectedDeckIds,
                    onSelectionChanged = { ids, checked ->
                        onAlarmChange(alarm.copy(selectedDeckIds = if (checked) alarm.selectedDeckIds + ids else alarm.selectedDeckIds - ids))
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Button(onClick = onConfirm, enabled = !loading && alarm.activeDays.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Enregistrer") }
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annuler") }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..4).forEach { day -> DayChip(day, day in selectedDays, onDayToggle, Modifier.weight(1f)) }
        }
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (5..7).forEach { day -> DayChip(day, day in selectedDays, onDayToggle, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayChip(day: Int, selected: Boolean, onDayToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = { onDayToggle(day) },
        label = { Text(dayLabels.getValue(day).take(3), modifier = Modifier.fillMaxWidth()) },
        modifier = modifier,
    )
}

@Composable
private fun DiagnosticSection(
    loading: Boolean,
    onReadDueCards: () -> Unit,
    onOpenAnkiDroid: () -> Unit,
    snapshot: DueCardsSnapshot?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Diagnostic", style = MaterialTheme.typography.headlineSmall)
        Text("Vérifie la connexion avec AnkiDroid avant une session.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        Row(modifier = Modifier.padding(top = 10.dp)) {
            OutlinedButton(onClick = onReadDueCards, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Lire les cartes") }
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedButton(onClick = onOpenAnkiDroid, modifier = Modifier.weight(1f)) { Text("Ouvrir AnkiDroid") }
        }
        snapshot?.let { DueSummary(it) }
    }
}

@Composable
private fun ColumnScope.DueSummary(snapshot: DueCardsSnapshot) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cartes dues : ${snapshot.total}", style = MaterialTheme.typography.titleMedium)
            snapshot.decks.sortedBy { it.name }.forEach { deck ->
                Text("${deck.name} : ${deck.cardCount}", modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private val dayLabels = mapOf(
    1 to "Lundi", 2 to "Mardi", 3 to "Mercredi", 4 to "Jeudi",
    5 to "Vendredi", 6 to "Samedi", 7 to "Dimanche",
)

private fun formatDays(days: Set<Int>): String =
    if (days.isEmpty()) "Aucun jour" else days.sorted().joinToString(" · ") { dayLabels.getValue(it).take(3) }

internal fun toggleDay(selectedDays: Set<Int>, day: Int): Set<Int> =
    if (day in selectedDays) selectedDays - day else selectedDays + day

private fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun formatOccurrence(value: ZonedDateTime?): String = value?.format(
    DateTimeFormatter.ofPattern("EEE dd/MM à HH:mm z", Locale.getDefault()),
) ?: "Aucune occurrence"

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
            DeckTreeRow(node, 0, expandedPaths, { path -> expandedPaths = if (path in expandedPaths) expandedPaths - path else expandedPaths + path }, selectedDeckIds, onSelectionChanged)
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
        modifier = Modifier.fillMaxWidth().clickable(enabled = hasChildren) { onExpandToggle(node.path) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (!hasChildren) "" else if (expanded) "⌄" else "›", modifier = Modifier.padding(start = (level * 20).dp).size(20.dp))
        node.deck?.let {
            androidx.compose.material3.Checkbox(
                checked = nodeDeckIds.all { it in selectedDeckIds },
                onCheckedChange = { checked -> onSelectionChanged(nodeDeckIds, checked) },
            )
        }
        Text(node.name, fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(start = 8.dp).weight(1f))
        Text(node.dueCount.toString(), fontWeight = if (hasChildren) FontWeight.Bold else FontWeight.Normal)
    }
    if (expanded) node.children.forEach { child -> DeckTreeRow(child, level + 1, expandedPaths, onExpandToggle, selectedDeckIds, onSelectionChanged) }
}

private fun DeckTreeNode.allDeckIds(): Set<String> = buildSet {
    deck?.let { add(it.identifier) }
    children.forEach { addAll(it.allDeckIds()) }
}

private fun buildDeckTree(decks: List<AnkiDeck>, dueCountsByDeckId: Map<String, Int>): List<DeckTreeNode> {
    class MutableNode(val name: String, val path: String, var deck: AnkiDeck? = null, val children: MutableMap<String, MutableNode> = sortedMapOf())
    val roots = sortedMapOf<String, MutableNode>()
    decks.forEach { deck ->
        var currentChildren: MutableMap<String, MutableNode> = roots
        var path = ""
        val parts = deck.name.split("::")
        parts.forEachIndexed { index, part ->
            path = if (path.isEmpty()) part else "$path::$part"
            val node = currentChildren.getOrPut(part) { MutableNode(part, path) }
            if (index == parts.lastIndex) node.deck = deck
            currentChildren = node.children
        }
    }
    fun convert(nodes: Collection<MutableNode>): List<DeckTreeNode> = nodes.sortedBy { it.name }.map { node ->
        val children = convert(node.children.values)
        DeckTreeNode(node.name, node.path, node.deck, (node.deck?.let { dueCountsByDeckId[it.identifier] ?: 0 } ?: 0) + children.sumOf { it.dueCount }, children)
    }
    return convert(roots.values)
}

private fun expandParentSelection(decks: List<AnkiDeck>, selectedDeckIds: Set<String>): Set<String> {
    val selectedNames = decks.filter { it.identifier in selectedDeckIds }.map { it.name }
    return selectedDeckIds + decks.filter { deck -> selectedNames.any { parent -> deck.name == parent || deck.name.startsWith("$parent::") } }.map { it.identifier }
}
