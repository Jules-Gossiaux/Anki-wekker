# Roadmap

## Phase 0 — cadrage documentaire

Créer la documentation, les règles de projet et le socle Git. **Terminée.**

## Phase 1 — spike AnkiDroid

- créer un projet Android minimal ; **terminé**
- afficher les permissions et versions ; **partiel**
- découvrir les decks ; **prototype terminé, noms affichés avec fallback sur l'identifiant**
- calculer et afficher un compteur dû pour une sélection ; **prototype terminé, sélection persistante**
- ouvrir AnkiDroid ; **terminé**
- documenter les cas non accessibles ; **terminé dans le contrat, validation appareil restante**
- produire une validation sur le OnePlus 10 Pro. **sélection et compteur validés manuellement ; alarme validée sur appareil**
- rendre la vue de sélection défilable sur les collections longues ; **terminé**

## Phase 2 — alarme minimale

- créer une alarme unique ; **prototype quotidien livré, heure persistée**
- déclencher audio/vibration ; **sonnerie en boucle et notification haute priorité livrées**
- ouvrir AnkiDroid ; **ouverture automatique via écran d'alerte livrée**
- cycle borné et notification ; **notification et écran plein écran validés ; arrêt automatique sur compteur restant à implémenter**
- restaurer l'alarme après redémarrage. **reprogrammation quotidienne livrée ; test après redémarrage à compléter**

## Phase 3 — session conditionnelle

- relire le compteur ;
- arrêter à zéro ;
- relancer après inactivité ;
- gérer sortie d'AnkiDroid ;
- gérer indisponibilité et fallback.

## Phase 4 — produit MVP

- alarmes multiples ;
- jours actifs ;
- sélection persistante de decks avec confirmation explicite ; **prototype livré en Phase 1**
- écran diagnostic ;
- réglages audio, vibration et délais ;
- tests manuels complets OxygenOS.

## Après le MVP

- meilleure détection de l'application au premier plan ;
- widgets ou raccourcis ;
- import/export de configuration ;
- étude de publication privée ou Play Store.

## Garde-fous

Ne pas commencer la Phase 2 si le compteur AnkiDroid n'est pas fiable. Ne pas présenter une permission ou un contournement OEM comme une garantie. Ne pas ajouter de multi-plateforme avant la validation du flux Android complet.
