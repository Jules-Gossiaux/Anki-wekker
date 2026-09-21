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

## ADR-0007 — Résolution des noms de decks avec fallback

**Statut :** accepté.

AnkiDroid 2.24 expose un endpoint `decks/` côté provider qui n'est pas encore représenté de manière stable dans le contrat public consommé par le projet. L'application tente donc de résoudre les noms par colonnes candidates (`deck_id`/`id`/`_id` et `name`/`deck_name`) et conserve l'identifiant comme fallback. Aucun accès direct à la base AnkiDroid n'est ajouté.

## ADR-0008 — Modèle multi-alarmes

**Statut :** accepté.

Chaque alarme possède un identifiant stable, une heure, des jours actifs, un fuseau IANA et une sélection de decks. L'ancienne alarme unique est migrée vers une première alarme afin de préserver la configuration existante.

## ADR-0009 — Alarmes chevauchantes

**Statut :** accepté.

Les alarmes simultanées partagent le même service de premier plan, la même notification et la même sortie audio. Elles conservent néanmoins des compteurs, sélections et états séparés. Le service s'arrête uniquement quand toutes les sessions sont terminées.

## ADR-0010 — Fuseau horaire des alarmes

**Statut :** accepté.

Une alarme conserve le fuseau IANA présent lors de sa création ou modification. Les occurrences sont recalculées avec `java.time` et les alarmes sont reprogrammées après un changement de fuseau ou d'heure système. Une heure locale située dans un trou DST est avancée par les règles Java du fuseau.

## Questions ouvertes

- Quelle version exacte d'AnkiDroid sera la version minimale supportée ?
- Les cartes d'apprentissage momentanément non disponibles doivent-elles être exclues du compteur ?
- Le délai de relance doit-il être identique pour toutes les alarmes ou configurable par alarme ?
