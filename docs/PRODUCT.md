# Produit

## Vision

Transformer le téléphone en réveil d'étude : une alarme réveille l'utilisateur, puis reste conditionnellement active jusqu'à ce que toutes les cartes dues des decks choisis aient été révisées dans AnkiDroid.

## Utilisateur cible

L'utilisateur principal est le propriétaire du OnePlus 10 Pro. Le produit n'a pas encore d'objectif de distribution publique.

## Fonctionnement principal

Une alarme possède une heure, un ensemble de jours et une sélection de decks AnkiDroid. À son déclenchement, l'application lit le nombre de cartes dues ciblées et propose d'ouvrir AnkiDroid. Tant que ce nombre n'est pas zéro, la session reste inachevée.

Si aucune interaction n'est détectée pendant un délai configurable, dix secondes par défaut, le son peut être relancé. Si AnkiDroid n'est plus au premier plan, l'application peut relancer une alerte. La durée d'un cycle sonore est limitée, notamment lorsque l'état AnkiDroid est indisponible.

## Périmètre MVP

- alarmes récurrentes et activation/désactivation individuelle ;
- choix des jours, de l'heure et de plusieurs decks par alarme ;
- support des decks et sous-decks ;
- cartes dues uniquement : révisions et cartes d'apprentissage actuellement dues, selon le contrat confirmé par la spike ;
- ouverture d'AnkiDroid vers la révision ;
- compteur initial, suivi et arrêt à zéro ;
- son, vibration et notification d'alarme ;
- fonctionnement écran verrouillé si Android l'autorise ;
- réglage du délai d'inactivité et de la durée d'un cycle sonore ;
- installation locale par Android Studio/ADB.

## Hors périmètre

- cartes nouvelles non dues ;
- modification de la planification Anki ;
- copie locale de la collection ;
- synchronisation ou compte ;
- iOS ;
- publication Play Store ;
- contournement du bouton d'arrêt ou comportement malveillant ;
- garantie lorsque le téléphone est éteint.

## Critères d'acceptation

1. Une alarme désactivée ne déclenche rien.
2. Les jours configurés déterminent les occurrences de l'alarme.
3. Une alarme sans carte due ciblée ne maintient pas de session active.
4. Une session active ne se termine qu'après confirmation d'un compteur égal à zéro.
5. Une mauvaise réponse qui rend une carte à nouveau due empêche la fin tant que le compteur reste positif.
6. Quitter AnkiDroid ou rester inactif déclenche une alerte selon les permissions et le comportement Android validé.
7. Si l'intégration est indisponible, l'application sonne pendant une durée bornée et affiche une action de reprise ; elle ne boucle pas indéfiniment sans état vérifiable.
