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
    val cardCount: Int,
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

    suspend fun readDueCards(): AnkiDroidResult<DueCardsSnapshot> = withContext(Dispatchers.IO) {
        if (!isInstalled()) {
            return@withContext AnkiDroidResult.Failure("AnkiDroid n'est pas installé.")
        }

        try {
            val decks = mutableMapOf<String, Int>()
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
                    val deck = cursor.textOrUnknown(CARD_DECK_ID)
                    decks[deck] = (decks[deck] ?: 0) + 1
                }
            }

            AnkiDroidResult.Success(
                DueCardsSnapshot(
                    total = decks.values.sum(),
                    decks = decks.entries
                        .sortedBy { it.key }
                        .map { AnkiDeckSnapshot(it.key, it.value) },
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

    private fun Cursor.textOrUnknown(column: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else "Deck inconnu"
    }

    companion object {
        const val PACKAGE_NAME = "com.ichi2.anki"
        const val READ_WRITE_PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private const val AUTHORITY = "com.ichi2.anki.flashcards"
        private val CARDS_URI = Uri.parse("content://$AUTHORITY/cards")
        private const val CARD_ID = "_id"
        private const val CARD_DECK_ID = "deck_id"
        private const val CARD_QUEUE = "queue"
        private const val CARD_DUE = "due"
    }
}
