package com.julesgossiaux.ankiwekker.permissions

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.permissionOnboardingDataStore by preferencesDataStore(name = "permission_onboarding")

class PermissionOnboardingStore(private val context: Context) {
    private val seenKey = booleanPreferencesKey("seen")

    suspend fun hasBeenSeen(): Boolean =
        context.permissionOnboardingDataStore.data.first()[seenKey] ?: false

    suspend fun markSeen() {
        context.permissionOnboardingDataStore.edit { preferences -> preferences[seenKey] = true }
    }
}
