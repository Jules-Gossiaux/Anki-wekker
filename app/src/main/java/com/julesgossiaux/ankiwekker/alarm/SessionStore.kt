package com.julesgossiaux.ankiwekker.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "study_session")

class SessionStore(private val context: Context) {
    private val activeAlarmIdsKey = stringSetPreferencesKey("active_alarm_ids")

    suspend fun readActiveAlarmIds(): Set<String> =
        context.sessionDataStore.data.first()[activeAlarmIdsKey].orEmpty()

    suspend fun saveActiveAlarmIds(alarmIds: Set<String>) {
        context.sessionDataStore.edit { preferences ->
            if (alarmIds.isEmpty()) {
                preferences.remove(activeAlarmIdsKey)
            } else {
                preferences[activeAlarmIdsKey] = alarmIds
            }
        }
    }
}
