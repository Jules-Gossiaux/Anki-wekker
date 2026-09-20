# Intégration AnkiDroid

## Contrat visé

L'adaptateur doit fournir :

```kotlin
interface AnkiDroidGateway {
    suspend fun isAvailable(): Boolean
    suspend fun listDecks(): List<DeckRef>
    suspend fun countDueCards(selection: DeckSelection): DueCount
    fun openReview(selection: DeckSelection): OpenReviewResult
}
```

Les noms sont indicatifs et ne constituent pas encore une API implémentée.

## Sémantique du compteur

Le compteur de l'alarme porte sur les cartes dues au moment de la lecture. Il ne compte pas les nouvelles cartes non dues. Les cartes d'apprentissage ou de réapprentissage doivent être incluses seulement si AnkiDroid les considère actuellement disponibles/dues selon le comportement documenté et validé par tests.

Le compteur peut augmenter après une réponse « Again » ou lorsqu'une carte revient dans la file. L'application ne mémorise pas un nombre décrémenté artificiellement : elle relit l'état AnkiDroid.

## Sous-decks

Une sélection peut viser un deck seul ou un deck parent avec ses descendants. L'identité persistée doit être un identifiant de deck stable si l'API le fournit ; le nom affiché ne doit pas être l'unique clé.

## Rafraîchissement

Le compteur est relu :

- au déclenchement ;
- après le retour au premier plan ;
- après une réponse ou une reprise de session si l'API permet de l'observer ;
- à intervalles bornés pendant une session ;
- avant de déclarer l'alarme terminée.

## Fallbacks

- AnkiDroid absent : afficher une installation/reprise nécessaire et sonner pendant la durée bornée.
- API non disponible : état `Unavailable`, aucune déclaration de complétion.
- deck supprimé ou renommé : signaler la sélection invalide et demander une correction.
- synchronisation/collection verrouillée : réessayer selon une politique bornée.

## Implémentation actuelle

La première tranche contient `AnkiDroidGateway`. Elle utilise le provider `content://com.ichi2.anki.flashcards/cards`, la requête Anki `is:due`, et expose un diagnostic minimal dans l'application. Elle demande maintenant explicitement la permission Android dangereuse `com.ichi2.anki.permission.READ_WRITE_DATABASE` avant la lecture. Pour AnkiDroid 2.24+, elle tente aussi `content://com.ichi2.anki.flashcards/decks/` afin de convertir les identifiants en noms. Si cet endpoint est absent ou refuse la requête, les identifiants restent affichés comme fallback.

L'écran diagnostic permet de charger les decks, de sélectionner un ou plusieurs identifiants et de mémoriser cette sélection localement avec DataStore. Une sélection vide signifie « tous les decks ». Le filtre est appliqué après la requête `is:due`, sans modifier la collection AnkiDroid.

Les résultats sont classés selon la convention Anki des sous-decks (`Parent::Enfant`). Le total d'un deck parent inclut les cartes dues de ses descendants, tandis qu'un sous-deck est affiché avec une indentation visuelle.

Cette implémentation est une spike : elle doit être vérifiée sur le téléphone cible et avec plusieurs versions/états de collection avant d'être utilisée par le moteur d'alarme.

## Spike de preuve

Le premier code doit contenir un écran diagnostic permettant d'afficher version d'AnkiDroid, decks découverts, sélection testée, compteur brut, compteur interprété et erreur éventuelle. Cette sortie facilitera la validation manuelle sans exposer le contenu des cartes.
