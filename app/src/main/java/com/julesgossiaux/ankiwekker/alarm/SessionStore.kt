package com.julesgossiaux.ankiwekker.alarm

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "study_session")

class SessionStore(private val context: Context) {
    private val activeKey = booleanPreferencesKey("active")

    suspend fun isActive(): Boolean =
        context.sessionDataStore.data.first()[activeKey] ?: false

    suspend fun setActive(active: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[activeKey] = active
        }
    }
}
