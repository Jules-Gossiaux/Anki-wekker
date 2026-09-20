package com.julesgossiaux.ankiwekker.alarm

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.alarmDataStore by preferencesDataStore(name = "alarm")

class AlarmStore(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val hourKey = intPreferencesKey("hour")
    private val minuteKey = intPreferencesKey("minute")

    suspend fun read(): AlarmSettings {
        val preferences = context.alarmDataStore.data.first()
        return AlarmSettings(
            enabled = preferences[enabledKey] ?: false,
            hour = preferences[hourKey] ?: 7,
            minute = preferences[minuteKey] ?: 0,
        )
    }

    suspend fun save(settings: AlarmSettings) {
        context.alarmDataStore.edit { preferences ->
            preferences[enabledKey] = settings.enabled
            preferences[hourKey] = settings.hour
            preferences[minuteKey] = settings.minute
        }
    }
}
