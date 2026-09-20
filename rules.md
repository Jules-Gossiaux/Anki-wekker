# Règles du projet

## Principes

- L'application est personnelle, locale et Android-only pour le MVP.
- AnkiDroid est la source de vérité : ne pas copier, modifier ou rescheduler les cartes.
- Ne jamais prétendre qu'un comportement est fiable sans test sur le OnePlus cible.
- Distinguer dans la documentation les exigences confirmées, les propositions, les limites et les questions ouvertes.
- Préférer le plus petit incrément vérifiable.
- Ne pas ajouter de compte, serveur, télémétrie ou traitement distant sans décision explicite.

## Intégration AnkiDroid

- Utiliser l'API publique ou le `ContentProvider` documenté d'AnkiDroid autant que possible.
- Encapsuler l'intégration derrière un adaptateur testable.
- Ne jamais accéder directement à la base SQLite privée d'AnkiDroid sans décision documentée.
- Ne pas compter une alarme comme terminée sur la seule ouverture d'AnkiDroid.
- L'état terminé doit être confirmé par un compteur vérifié à zéro.
- Les erreurs, permissions manquantes et données indisponibles doivent avoir un état explicite.

## Alarmes Android

- Prévoir les permissions et réglages nécessaires, mais toujours fournir un comportement de repli.
- L'audio, la vibration et la notification doivent être pilotés par un service au premier plan lorsque nécessaire.
- Ne pas promettre un fonctionnement téléphone éteint.
- Tester verrouillage, redémarrage, force-stop, économie de batterie et restrictions OxygenOS.
- Toute surveillance de l'application au premier plan doit être justifiée par le comportement produit et documentée côté confidentialité.

## Code et tests

- Kotlin strict, architecture simple, logique métier indépendante de Compose.
- Pas de logique métier dans les composables.
- Les réglages persistés sont validés et versionnés.
- Toute nouvelle règle observable reçoit un test unitaire, d'intégration ou manuel documenté.
- Ne pas déclarer un test réussi s'il n'a pas été exécuté.
- Toute modification de comportement ou de limitation met à jour `docs/`.

## Git

- Ne pas travailler directement sur `main` pour une fonctionnalité ; utiliser une branche dédiée.
- Ne pas utiliser de reset destructif ni de force-push.
- Les commits doivent rester ciblés et explicites.
