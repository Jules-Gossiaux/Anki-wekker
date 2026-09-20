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

## Première alarme — validation à effectuer

- autoriser les alarmes exactes et les notifications ;
- choisir une heure située dans les prochaines minutes ;
- vérifier que l'alarme reste activée après fermeture de l'application ;
- vérifier la notification et son action « Ouvrir AnkiDroid » ;
- vérifier que l'écran d'alerte ouvre automatiquement AnkiDroid et que la sonnerie boucle ;
- vérifier que l'intention plein écran ouvre directement AnkiDroid lorsque l'application est en arrière-plan ou l'écran verrouillé ;
- vérifier que « Arrêter la sonnerie » coupe bien le son ;
- vérifier la reprogrammation quotidienne après le déclenchement ;
- tester écran verrouillé, mode silencieux et optimisation batterie OxygenOS.

### Validation appareil — 2026-09-20

- appareil : OnePlus 10 Pro ;
- système : OxygenOS 16 ;
- résultat : alarme déclenchée, sonnerie fonctionnelle, AnkiDroid ouvert automatiquement ;
- statut : validation manuelle réussie pour cette tranche ;
- reste à tester : redémarrage du téléphone, optimisation batterie et arrêt automatique après compteur à zéro.

## Session conditionnelle — validation à effectuer

- déclencher une alarme avec au moins une carte due sélectionnée ;
- vérifier que la sonnerie se coupe après environ 10 secondes puis revient si le compteur est toujours positif ;
- répondre aux cartes dans AnkiDroid et vérifier l'arrêt automatique lorsque le compteur atteint zéro ;
- répondre à quelques cartes sans atteindre zéro et vérifier que la sonnerie suivante est supprimée après la baisse du compteur ;
- laisser le compteur inchangé et vérifier que la sonnerie revient au cycle suivant ;
- après une première baisse du compteur, vérifier que les contrôles se font toutes les 5 secondes ;
- avant toute baisse, vérifier que le compteur est contrôlé toutes les 10 secondes ;
- pendant ce mode, vérifier qu'une nouvelle baisse maintient le silence et qu'une absence de baisse relance la sonnerie ;
- quitter AnkiDroid avec des cartes restantes et vérifier que le service continue sa surveillance ;
- tester une collection indisponible et vérifier que la session reste en état non complété ;
- utiliser l'action « Arrêter » uniquement comme arrêt manuel de secours.

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
- vérifier que chaque deck affiche son nombre de cartes dues et que les parents agrègent leurs sous-decks ;
- faire défiler la vue de sélection et vérifier que le bouton « Confirmer la sélection » est entièrement accessible ;
- sélectionner un deck parent et vérifier que ses sous-decks sont également inclus dans le total ;
- rouvrir une ancienne sélection contenant un parent et vérifier que ses sous-decks sont automatiquement repris ;
- modifier la sélection et vérifier que « Cartes dues sélectionnées » est recalculé immédiatement ;
- appuyer sur « Confirmer la sélection » et vérifier que la vue se referme ;
- modifier la sélection, confirmer, puis cliquer sur « Lire les cartes dues » et vérifier que le total utilise immédiatement les nouveaux decks ;
- lire les cartes dues puis rouvrir « Sélectionner les decks » et vérifier que la vue est de nouveau accessible ;
- vérifier que la lecture filtrée ignore les cartes dues des decks non sélectionnés ;
- vérifier que les résultats sont triés par nom, que les sous-decks sont indentés et que le parent agrège leurs cartes dues ;
- fermer puis rouvrir l'application et vérifier que la sélection est conservée ;
- désélectionner tous les decks et vérifier que le mode « tous les decks » est rétabli ;
- consigner la version d'AnkiDroid et tout écart dans cette section.
