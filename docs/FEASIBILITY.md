# Faisabilité

## Conclusion provisoire

Le produit est faisable comme application Android personnelle, mais la fiabilité dépend du contrat réel exposé par la version installée d'AnkiDroid et des restrictions d'OxygenOS. La première implémentation doit donc être une spike installable sur le OnePlus 10 Pro.

## AnkiDroid

AnkiDroid expose une API pour applications tierces et un `ContentProvider` de cartes. Les compteurs affichés par AnkiDroid distinguent notamment nouvelles cartes, apprentissage et révisions. Le projet doit vérifier si les données accessibles permettent de reproduire exactement « cartes dues ciblées » pour des decks sélectionnés et leurs sous-decks.

Points à vérifier :

- découverte des decks et de leur hiérarchie ;
- sélection d'un deck parent et inclusion des sous-decks ;
- définition exacte de `due` pour les cartes d'apprentissage et de révision ;
- disponibilité du compteur après une réponse dans AnkiDroid ;
- comportement pendant une synchronisation ou une collection verrouillée ;
- lancement d'AnkiDroid sur le bon deck ;
- erreur propre si AnkiDroid est absent ou si l'accès API est désactivé.

La base privée `collection.anki2` ne sera pas lue directement dans le MVP tant que l'API publique n'a pas été démontrée insuffisante.

## Android

Le réveil nécessitera probablement `AlarmManager`, une notification haute importance et un service au premier plan pendant une session. Les versions récentes d'Android restreignent les alarmes exactes et les notifications plein écran ; les permissions doivent être vérifiées à l'exécution.

Le téléphone éteint est une limite matérielle : aucune application ne peut jouer du son lorsque l'appareil n'est plus alimenté. Après redémarrage, la restauration des alarmes et du service devra être testée.

## OxygenOS 16 / OnePlus

Le test réel doit couvrir l'optimisation batterie, le démarrage automatique, le verrouillage, la mise en veille, le redémarrage et le force-stop. L'application doit afficher un diagnostic indiquant clairement quelles permissions ou exceptions système manquent.

## Détection de sortie d'AnkiDroid

Ordre de préférence :

1. confirmer l'activité de la session par des vérifications de compteur et de cycle de vie ;
2. utiliser une observation de l'application au premier plan si une permission acceptable est disponible ;
3. considérer `AccessibilityService` uniquement comme expérimentation séparée, car sa permission est sensible et non nécessaire pour le premier MVP.

## Spike d'acceptation

La spike est réussie si elle peut documenter, sur l'appareil cible : déclenchement d'une alarme, lecture des decks, calcul d'un compteur, ouverture d'AnkiDroid, rafraîchissement après réponse, sortie de l'application, écran verrouillé, redémarrage et restriction batterie. Chaque scénario doit indiquer « fiable », « approximatif », « impossible » ou « fallback ».

## Sources techniques

- [AnkiDroid API](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-API)
- [FlashCardsContract](https://github.com/ankidroid/Anki-Android/blob/main/api/src/main/java/com/ichi2/anki/FlashCardsContract.kt)
- [Android 14 full-screen intents](https://developer.android.com/about/versions/14/behavior-changes-14)
- [Foreground services](https://developer.android.com/develop/background-work/services/fgs/declare)
