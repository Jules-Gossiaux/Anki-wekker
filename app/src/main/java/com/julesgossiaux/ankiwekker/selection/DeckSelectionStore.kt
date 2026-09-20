package com.julesgossiaux.ankiwekker.selection

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deckSelectionDataStore by preferencesDataStore(name = "deck_selection")

class DeckSelectionStore(private val context: Context) {
    private val selectedDeckIdsKey = stringSetPreferencesKey("selected_deck_ids")

    suspend fun readSelectedDeckIds(): Set<String> =
        context.deckSelectionDataStore.data.first()[selectedDeckIdsKey].orEmpty()

    suspend fun saveSelectedDeckIds(deckIds: Set<String>) {
        context.deckSelectionDataStore.edit { preferences ->
            preferences[selectedDeckIdsKey] = deckIds
        }
    }
}
