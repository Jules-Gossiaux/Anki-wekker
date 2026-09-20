# Développement

## Prérequis

- Android Studio ;
- JDK fourni par Android Studio ;
- Android SDK et Platform-Tools ;
- téléphone OnePlus 10 Pro avec débogage USB activé ;
- AnkiDroid installé et contenant une collection de test.

## Vérification ADB

```powershell
adb devices
```

Le téléphone doit apparaître avec l'état `device`. Ne pas continuer si l'appareil est `unauthorized`.

## Stack prévue

- Kotlin ;
- Jetpack Compose ;
- Gradle Kotlin DSL ;
- DataStore ;
- JUnit et tests instrumentés Android ;
- aucune dépendance réseau requise.

## Workflow

1. Lire `rules.md` et la documentation pertinente.
2. Vérifier `git status` et la branche.
3. Implémenter une tranche réduite et testable.
4. Installer sur le téléphone réel.
5. Exécuter les tests ciblés et la validation manuelle.
6. Mettre à jour la documentation et signaler les limites.

## Première tranche

La première tranche est l'écran diagnostic AnkiDroid : permissions, disponibilité, liste des decks, sélection temporaire et compteur de cartes dues. Elle précède l'interface complète de réveil.
