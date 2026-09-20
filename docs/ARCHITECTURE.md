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

La première tranche d'alarme persiste une heure quotidienne dans `AlarmStore`, la programme avec `AlarmManager.setExactAndAllowWhileIdle` et reçoit l'événement dans `AlarmReceiver`. Le receiver crée une notification haute priorité avec une action vers AnkiDroid, puis reprogramme l'occurrence suivante. Le contrôle du compteur et le cycle sonore persistant restent dans la phase de session conditionnelle.

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

## Alarmes multiples

Chaque alarme est indépendante. Le planificateur calcule la prochaine occurrence de chaque alarme active. Si deux alarmes se chevauchent, l'application doit fusionner la surveillance pendant la même période mais conserver les sélections et les résultats séparés ; cette règle devra être confirmée par un test avant implémentation.

## Sécurité et confidentialité

Aucun contenu de carte, historique de révision ou identifiant externe ne quitte l'appareil. Les permissions sensibles sont expliquées dans l'interface. Les logs de diagnostic ne doivent pas contenir le contenu des cartes.
