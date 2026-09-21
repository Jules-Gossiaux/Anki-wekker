# Architecture

## Choix

Application Android native en Kotlin, avec Jetpack Compose. Ce choix réduit la distance avec `AlarmManager`, les services au premier plan, les notifications et l'intégration native AnkiDroid.

## Modules logiques

```text
UI Compose
  ├── Alarm configuration
  ├── Deck selection
  └── Diagnostic / permissions

Application layer
  ├── Alarm scheduling
  ├── Study session coordinator
  └── Permission coordinator

Domain
  ├── Alarm
  ├── Deck selection
  ├── Due-count contract
  └── Session state machine

Platform adapters
  ├── AlarmManager
  ├── Foreground alarm service
  ├── Notification/audio/vibration
  └── AnkiDroid adapter

Persistence
  └── DataStore for alarms and preferences
```

Chaque alarme est persistée dans `AlarmStore` avec un identifiant stable, une heure, des jours actifs, un fuseau IANA et sa sélection de decks. `AlarmScheduler` programme un `PendingIntent` distinct par identifiant et calcule la prochaine occurrence avec `java.time`, y compris les changements de fuseau et de DST. `AlarmReceiver` transmet l'identifiant au `StudySessionService`. Les alarmes qui se chevauchent partagent un seul service, une seule notification et une seule sonnerie, mais leurs compteurs et leurs sélections restent suivis séparément ; le service s'arrête lorsque toutes les sessions sont terminées. Une migration convertit l'ancienne alarme unique et sa sélection globale en première alarme multi-alarmes.

Les modifications d'une alarme sont enregistrées automatiquement à chaque changement valide : heure, jours actifs, decks et activation. Une nouvelle alarme n'est créée dans `AlarmStore` qu'à sa première modification ; fermer son éditeur avant toute modification l'abandonne. Fermer l'éditeur après une modification ne restaure pas l'ancienne valeur. La permission d'alarme exacte contrôle la planification système, mais ne bloque pas la sauvegarde de la configuration.

Le receiver lance `StudySessionService` et tente d'utiliser l'intention AnkiDroid comme intention plein écran de la notification ; cette ouverture reste non validée sur l'appareil cible. Avant tout progrès, le service relit le compteur ciblé toutes les 10 secondes et sonne par cycles de 10 secondes. Dès qu'une baisse est détectée, il passe en mode `StudyDetected` : vérification toutes les 5 secondes, silence si le compteur baisse, et sonnerie de 5 secondes dès qu'un contrôle constate une absence de progrès. Le son est routé vers le flux Android des alarmes. Le service tourne dans le processus dédié `:alarm`, séparé de l'interface. Après retrait de la tâche, `SessionWatchdog` programme directement un `PendingIntent` de service au premier plan à 500 ms, au lieu de demander à un receiver de lancer le service depuis l'arrière-plan — une opération refusée par Android sur l'appareil cible. Les alarmes sont reprogrammées après redémarrage, changement d'heure et changement de fuseau.

Sur l'appareil cible, OxygenOS peut néanmoins tuer le processus de premier plan lors d'un balayage dans les applications récentes. Le système est alors seul maître de la reprise `START_REDELIVER_INTENT` et les mesures ont montré un retard d'environ 45 secondes : une application ordinaire ne peut pas garantir une reprise à 0,5 seconde après ce type de suppression. L'utilisateur doit éviter de balayer l'application pendant une session et l'autoriser dans les réglages batterie/autolancement d'OxygenOS si ces options sont proposées. Un arrêt forcé explicite par Android interrompt également toujours la session.

## Source de vérité

AnkiDroid possède les cartes, les decks et la planification. L'application persiste seulement les alarmes, les préférences, la dernière session connue et les états de diagnostic nécessaires à l'interface.

## État de session

```text
Idle
  -> Triggered
  -> WaitingForAnki
  -> Studying
  -> InactiveAlert
  -> Studying
  -> Completed
  -> Unavailable
```

Une transition vers `Completed` exige une lecture valide du compteur ciblé à zéro. `Unavailable` est borné dans le temps et ne doit pas être confondu avec `Completed`.

La planification et le déclenchement de la première alarme sont validés sur le téléphone cible. La planification multi-alarmes est couverte localement par des tests d'occurrences ; sa validation appareil reste à effectuer.

## Alarmes multiples

Chaque alarme est indépendante. Le planificateur calcule la prochaine occurrence de chaque alarme active. Si deux alarmes se chevauchent, la surveillance, la notification et l'audio sont fusionnés dans le même service, tandis que les sélections et les résultats restent séparés. La session globale ne se termine que lorsque toutes les alarmes actives sont terminées.

## Sécurité et confidentialité

Aucun contenu de carte, historique de révision ou identifiant externe ne quitte l'appareil. Les permissions sensibles sont expliquées dans l'interface. Les logs de diagnostic ne doivent pas contenir le contenu des cartes.
