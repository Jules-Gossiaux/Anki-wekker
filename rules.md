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
- Toute feature, correction ou modification documentaire significative doit être développée sur une branche dédiée.
- Utiliser des commits fréquents, atomiques et Conventional Commits ; un commit doit représenter une unité vérifiable.
- Mettre à jour la documentation et les tests dans le même changement que le comportement concerné.
- Ouvrir une Pull Request pour chaque feature ou modification significative avant intégration dans `main`.
- Une PR doit expliquer le contexte, le comportement ajouté, les fichiers concernés, les tests exécutés, les limitations connues et les étapes de validation manuelle.
- Ne pas fusionner une PR dont les tests pertinents, la revue du diff ou la validation appareil requise ne sont pas terminés.
- Maintenir un suivi GitHub régulier : créer ou mettre à jour les issues, relier les PR aux issues et laisser les décisions importantes traçables dans les discussions ou la documentation.
- Ne jamais pousser de secrets, de clés, de collections Anki, de logs contenant du contenu de cartes ou de fichiers locaux Android.
- Si la création de PR n'est pas possible depuis l'environnement courant, laisser la branche et les commits prêts, puis signaler explicitement le blocage au lieu de prétendre qu'une PR existe.
