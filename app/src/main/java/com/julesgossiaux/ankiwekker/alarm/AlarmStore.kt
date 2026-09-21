package com.julesgossiaux.ankiwekker.alarm

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julesgossiaux.ankiwekker.selection.DeckSelectionStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.util.UUID

private val Context.alarmDataStore by preferencesDataStore(name = "alarm")

class AlarmStore(private val context: Context) {
    private val alarmsKey = stringPreferencesKey("alarms_v2")
    private val legacyEnabledKey = booleanPreferencesKey("enabled")
    private val legacyHourKey = intPreferencesKey("hour")
    private val legacyMinuteKey = intPreferencesKey("minute")

    suspend fun readAll(): List<AlarmSettings> {
        val preferences = context.alarmDataStore.data.first()
        preferences[alarmsKey]?.let { return decode(it) }

        val hasLegacyData = preferences[legacyEnabledKey] != null ||
            preferences[legacyHourKey] != null || preferences[legacyMinuteKey] != null
        if (!hasLegacyData) {
            saveAll(emptyList())
            return emptyList()
        }

        val migrated = AlarmSettings(
            enabled = preferences[legacyEnabledKey] ?: false,
            hour = (preferences[legacyHourKey] ?: 7).coerceIn(0, 23),
            minute = (preferences[legacyMinuteKey] ?: 0).coerceIn(0, 59),
            selectedDeckIds = DeckSelectionStore(context).readSelectedDeckIds(),
        )
        saveAll(listOf(migrated))
        return listOf(migrated)
    }

    suspend fun saveAll(alarms: List<AlarmSettings>) {
        context.alarmDataStore.edit { preferences ->
            preferences[alarmsKey] = encode(alarms)
        }
    }

    private fun encode(alarms: List<AlarmSettings>): String = JSONArray().apply {
        alarms.forEach { alarm ->
            put(JSONObject().apply {
                put("id", alarm.id)
                put("enabled", alarm.enabled)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("days", JSONArray(alarm.activeDays.toList()))
                put("zoneId", alarm.zoneId)
                put("decks", JSONArray(alarm.selectedDeckIds.toList()))
            })
        }
    }.toString()

    private fun decode(value: String): List<AlarmSettings> = runCatching {
        val json = JSONArray(value)
        buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    AlarmSettings(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        enabled = item.optBoolean("enabled"),
                        hour = item.optInt("hour", 7).coerceIn(0, 23),
                        minute = item.optInt("minute", 0).coerceIn(0, 59),
                        activeDays = item.optJSONArray("days").toIntSet().ifEmpty { (1..7).toSet() },
                        zoneId = item.optString("zoneId").takeIf {
                            it.isNotBlank() && runCatching { ZoneId.of(it) }.isSuccess
                        } ?: ZoneId.systemDefault().id,
                        selectedDeckIds = item.optJSONArray("decks").toStringSet(),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun JSONArray?.toIntSet(): Set<Int> = if (this == null) emptySet() else buildSet {
        for (index in 0 until length()) add(optInt(index))
    }.filter { it in 1..7 }.toSet()

    private fun JSONArray?.toStringSet(): Set<String> = if (this == null) emptySet() else buildSet {
        for (index in 0 until length()) add(optString(index))
    }.filter { it.isNotBlank() }.toSet()
}
