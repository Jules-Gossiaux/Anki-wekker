# Tests

## Unitaires

- calcul de la prochaine occurrence d'une alarme ;
- jours actifs, fuseaux horaires et changement d'heure ;
- machine d'état de session ;
- bornage du délai d'inactivité et du cycle sonore ;
- sélection de decks et inclusion des sous-decks ;
- distinction `Completed` / `Unavailable` ;
- fusion ou non de sessions concurrentes.

## Intégration Android

- création, modification, activation et désactivation d'alarmes ;
- persistance après relance ;
- restauration après redémarrage ;
- notification et service au premier plan ;
- permission d'alarme exacte et notification ;
- audio/vibration avec écran verrouillé.

## Intégration AnkiDroid

- AnkiDroid installé, ouvert et accessible ;
- AnkiDroid absent ;
- API désactivée ou version incompatible ;
- deck seul, deck parent et sous-decks ;
- zéro, une et plusieurs cartes dues ;
- réponse qui fait revenir une carte ;
- synchronisation ou collection indisponible ;
- suppression ou renommage d'un deck sélectionné.

## Matrice appareil cible

Chaque cas doit être testé sur le OnePlus 10 Pro sous OxygenOS 16 :

- écran actif ;
- écran verrouillé ;
- application en arrière-plan ;
- AnkiDroid quitté ;
- téléphone redémarré ;
- application arrêtée par force-stop ;
- optimisation batterie active puis désactivée ;
- mode silencieux et volume d'alarme ;
- absence de cartes dues ;
- plusieurs alarmes à des heures différentes.

## Critère de preuve

Une fonctionnalité native n'est déclarée fiable qu'après test sur l'appareil cible et consignation de la date, version Android/OxygenOS, permissions et résultat. Les limites OEM restent explicitement documentées.
