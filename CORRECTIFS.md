# Correctifs de revue (étapes 2 et 3a)

Fusionne ce dossier dans la racine du projet, puis **supprime les 4 fichiers listés en bas**.

## Bugs corrigés

| # | Gravité | Problème | Correction |
|---|---|---|---|
| 1 | Haute | `POST /factures/generation` avec `niveauId` facturait **tous** les élèves acceptés de l'établissement, pas ceux du niveau | Filtre sur le niveau de la classe de l'inscription + vérification que le niveau existe |
| 2 | Haute | Un **enseignant** (périmètre ETABLISSEMENT) pouvait lire toutes les factures, les exonérations et leurs motifs (bourses, aide sociale) | Nouvelles règles `peutConsulterFactures` (direction / personnel admin) et `peutConsulterBilan` (+ ministère / direction régionale, synthèse sans noms) |
| 3 | Haute | Le front appelait `POST …/suppression` pour salles et créneaux, et `GET /api/classes/{id}/emploi-du-temps` : **ces routes n'existent pas** côté backend | `api.delete` ajouté au client ; le front appelle les vraies routes `DELETE` ; route `/emploi-du-temps` (DTO allégé) ajoutée |
| 4 | Moyenne | Une classe pouvait avoir deux cours **en même temps** (seuls enseignant et salle étaient contrôlés) | Contrôle de chevauchement par classe (création et modification) |
| 5 | Moyenne | Devise `XOF` codée en dur (entités, service, migration, front) alors que le projet n'a pas de pays cible | Devise obligatoire (code ISO) à la création d'un frais, plus de défaut ; migration `V6` ; formatage `Intl` selon la devise |
| 6 | Moyenne | Devises mélangées possibles dans une même année → bilan faux | Une seule devise par établissement et par année ; devise non modifiable si des factures existent |
| 7 | Moyenne | `typeExoneration` libre → violation de la contrainte SQL → erreur 500 | Validation (BOURSE, REMISE, AIDE_SOCIALE, CORRECTION) → 400 lisible |
| 8 | Basse | 2 requêtes SQL supplémentaires par facture (existence de la table `paiement`) | Résultat mémorisé |
| 9 | Basse | Bilan : `nombreFactures` comptait les annulées | Exclues |
| 10 | Basse | Les factures annulées apparaissaient chez l'élève et le parent | Filtrées |
| 11 | Basse | `lister` frais sans année : `findAll()` filtré en mémoire | Requête dédiée |
| 12 | Basse | Fichiers en double : `api/emploisDuTemps.ts`, `api/facturation.ts` (déjà dans `endpoints.ts`), `types-*.ts` (déjà dans `types.ts`) | Supprimés, imports unifiés sur `api/endpoints` |

## Tests ajoutés
- `PerimetreFinancesServiceTest` (4 tests)
- `FacturationGenerationTest` (3 tests : filtre par niveau, idempotence, type d'exonération)

## À supprimer à la main
- `frontend/src/api/emploisDuTemps.ts`
- `frontend/src/api/facturation.ts`
- `frontend/src/types-emplois-du-temps.ts`
- `frontend/src/types-facturation.ts`

## Points à connaître (non modifiés)
- La génération concurrente de deux lots simultanés peut échouer sur l'index unique (erreur 409) : sans gravité, on relance.
- Une facture annulée n'est pas régénérée (index unique frais/élève) : volontaire pour l'audit.
- Les montants du front sont des `number` : OK pour l'affichage, à ne pas utiliser pour calculer des totaux officiels.
