package com.julesgossiaux.ankiwekker.ankidroid

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AnkiDeckSnapshot(
    val identifier: String,
    val name: String,
    val cardCount: Int,
)

data class AnkiDeck(
    val identifier: String,
    val name: String,
)

data class DueCardsSnapshot(
    val total: Int,
    val decks: List<AnkiDeckSnapshot>,
)

sealed interface AnkiDroidResult<out T> {
    data class Success<T>(val value: T) : AnkiDroidResult<T>
    data class Failure(val message: String) : AnkiDroidResult<Nothing>
}

class AnkiDroidGateway(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun hasDatabasePermission(): Boolean =
        context.checkSelfPermission(READ_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun isInstalled(): Boolean = runCatching {
        context.packageManager.getApplicationInfo(PACKAGE_NAME, 0)
    }.isSuccess

    fun openAnkiDroid(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    suspend fun listDecks(): AnkiDroidResult<List<AnkiDeck>> = withContext(Dispatchers.IO) {
        if (!isInstalled()) {
            return@withContext AnkiDroidResult.Failure("AnkiDroid n'est pas installé.")
        }

        try {
            AnkiDroidResult.Success(readDecks())
        } catch (error: SecurityException) {
            AnkiDroidResult.Failure("Accès AnkiDroid refusé : ${error.message ?: "permission manquante"}")
        } catch (error: IllegalStateException) {
            AnkiDroidResult.Failure("La collection AnkiDroid n'est pas disponible : ${error.message ?: "état invalide"}")
        } catch (error: RuntimeException) {
            AnkiDroidResult.Failure("Erreur de lecture des decks : ${error.message ?: error.javaClass.simpleName}")
        }
    }

    suspend fun readDueCards(selectedDeckIds: Set<String> = emptySet()): AnkiDroidResult<DueCardsSnapshot> = withContext(Dispatchers.IO) {
        if (!isInstalled()) {
            return@withContext AnkiDroidResult.Failure("AnkiDroid n'est pas installé.")
        }

        try {
            val decks = mutableMapOf<String, Int>()
            val deckNames = readDeckNames()
            val projection = arrayOf(
                CARD_ID,
                CARD_DECK_ID,
                CARD_QUEUE,
                CARD_DUE,
            )

            contentResolver.query(
                CARDS_URI,
                projection,
                "is:due",
                null,
                null,
            ).use { cursor ->
                if (cursor == null) {
                    return@withContext AnkiDroidResult.Failure("AnkiDroid n'a pas retourné de données.")
                }

                while (cursor.moveToNext()) {
                    val deckId = cursor.textOrUnknown(CARD_DECK_ID)
                    if (selectedDeckIds.isEmpty() || deckId in selectedDeckIds) {
                        decks[deckId] = (decks[deckId] ?: 0) + 1
                    }
                }
            }

            AnkiDroidResult.Success(
                DueCardsSnapshot(
                    total = decks.values.sum(),
                    decks = decks.entries
                        .sortedBy { it.key }
                        .map { (id, count) ->
                            AnkiDeckSnapshot(id, deckNames[id] ?: id, count)
                        },
                ),
            )
        } catch (error: SecurityException) {
            AnkiDroidResult.Failure("Accès AnkiDroid refusé : ${error.message ?: "permission manquante"}")
        } catch (error: IllegalStateException) {
            AnkiDroidResult.Failure("La collection AnkiDroid n'est pas disponible : ${error.message ?: "état invalide"}")
        } catch (error: RuntimeException) {
            AnkiDroidResult.Failure("Erreur de lecture AnkiDroid : ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun readDecks(): List<AnkiDeck> = readDeckNames()
        .map { (identifier, name) -> AnkiDeck(identifier, name) }
        .sortedBy { it.name }

    private fun readDeckNames(): Map<String, String> = runCatching {
        val result = mutableMapOf<String, String>()
        contentResolver.query(DECKS_URI, null, null, null, null).use { cursor ->
            if (cursor == null) return@runCatching result

            val idColumn = cursor.findColumn("deck_id", "id", "_id", "did")
            val nameColumn = cursor.findColumn("name", "deck_name", "deckName")
            if (idColumn == null || nameColumn == null) return@runCatching result

            while (cursor.moveToNext()) {
                val id = cursor.textOrUnknown(idColumn)
                val name = cursor.textOrUnknown(nameColumn)
                if (id != "Deck inconnu" && name != "Deck inconnu") {
                    result[id] = name
                }
            }
        }
        result
    }.getOrDefault(emptyMap())

    private fun Cursor.findColumn(vararg candidates: String): String? =
        candidates.firstOrNull { getColumnIndex(it) >= 0 }

    private fun Cursor.textOrUnknown(column: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else "Deck inconnu"
    }

    companion object {
        const val PACKAGE_NAME = "com.ichi2.anki"
        const val READ_WRITE_PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private const val AUTHORITY = "com.ichi2.anki.flashcards"
        private val CARDS_URI = Uri.parse("content://$AUTHORITY/cards")
        private val DECKS_URI = Uri.parse("content://$AUTHORITY/decks/")
        private const val CARD_ID = "_id"
        private const val CARD_DECK_ID = "deck_id"
        private const val CARD_QUEUE = "queue"
        private const val CARD_DUE = "due"
    }
}
