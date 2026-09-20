# Décisions

## ADR-0001 — Application Android native

**Statut :** accepté.

Kotlin natif et Jetpack Compose sont retenus pour faciliter l'accès aux mécanismes Android critiques et l'installation directe via Android Studio/ADB. Une stack multiplateforme pourrait être étudiée plus tard, mais elle n'est pas adaptée au premier risque technique.

## ADR-0002 — AnkiDroid comme source de vérité

**Statut :** accepté.

L'application ne duplique pas la collection et ne recode pas l'algorithme de planification. Elle lit les decks et les cartes dues via les interfaces publiques disponibles.

## ADR-0003 — Cartes dues uniquement

**Statut :** accepté.

Les nouvelles cartes non dues sont exclues. Les cartes d'apprentissage/réapprentissage sont incluses uniquement selon la sémantique de disponibilité confirmée dans la spike AnkiDroid.

## ADR-0004 — Nombre relu, pas compteur local

**Statut :** accepté.

Le nombre restant est recalculé depuis AnkiDroid. Cela prend en charge les cartes qui réapparaissent après une mauvaise réponse et évite les divergences causées par un compteur local.

## ADR-0005 — Alarme bornée en cas d'indisponibilité

**Statut :** accepté.

Si l'application ne peut pas confirmer l'état AnkiDroid, elle sonne pendant un cycle limité puis demande une action. Elle ne sonne pas indéfiniment sans information vérifiable.

## ADR-0006 — Usage personnel d'abord

**Statut :** accepté.

Le premier objectif est une installation locale sur le OnePlus 10 Pro. La publication Play Store, ses déclarations de permissions et une compatibilité multi-appareils sont reportées.

## Questions ouvertes

- Quelle version exacte d'AnkiDroid sera la version minimale supportée ?
- Les cartes d'apprentissage momentanément non disponibles doivent-elles être exclues du compteur ?
- Comment fusionner deux alarmes qui se déclenchent simultanément ?
- Le délai de relance doit-il être identique pour toutes les alarmes ou configurable par alarme ?
