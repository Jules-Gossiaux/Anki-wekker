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

## Première tranche — validation à effectuer

- ouvrir le projet dans Android Studio et synchroniser Gradle ;
- installer l'APK de debug sur le OnePlus ;
- vérifier l'autorisation AnkiDroid `READ_WRITE_DATABASE` ;
- vérifier que le premier clic demande cette autorisation et que le second clic lit les cartes après acceptation ;
- vérifier qu'un clic sur « Lire les cartes dues » retourne un état explicite ;
- comparer le total avec les compteurs AnkiDroid pour un deck sans sous-deck puis avec une sélection multi-decks ;
- vérifier le comportement avec zéro carte due, avec une carte due et après une réponse dans AnkiDroid ;
- vérifier le bouton « Ouvrir AnkiDroid » ;
- appuyer sur « Sélectionner les decks », vérifier l'arborescence repliable et sélectionner un deck ;
- appuyer sur « Confirmer la sélection » et vérifier que la vue se referme ;
- modifier la sélection, confirmer, puis cliquer sur « Lire les cartes dues » et vérifier que le total utilise immédiatement les nouveaux decks ;
- lire les cartes dues puis rouvrir « Sélectionner les decks » et vérifier que la vue est de nouveau accessible ;
- vérifier que la lecture filtrée ignore les cartes dues des decks non sélectionnés ;
- vérifier que les résultats sont triés par nom, que les sous-decks sont indentés et que le parent agrège leurs cartes dues ;
- fermer puis rouvrir l'application et vérifier que la sélection est conservée ;
- désélectionner tous les decks et vérifier que le mode « tous les decks » est rétabli ;
- consigner la version d'AnkiDroid et tout écart dans cette section.
