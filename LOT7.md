# Lot 7 — Direction régionale

À fusionner après `lot6-ministere.zip`. Aucune migration.

## Backend
- `GET /api/region/synthese?anneeScolaireId=` (rôle `DIRECTION_REGIONALE`) : établissements par statut, classes, élèves inscrits **de ses zones et sous-zones uniquement**. Seul le rôle DIRECTION_REGIONALE définit ce périmètre : un autre rôle du même compte ne l'élargit pas.
- `POST /api/zones/{id}/direction-regionale` (ministère) : crée le compte (changement de mot de passe forcé).
- La création de comptes est factorisée dans `AdministrationService.creerCompte`.

## Front
- Page `/region` (menu « Ma région », ouverte par défaut à la connexion) : chiffres clés + liste des établissements de la zone, en lecture seule.
- Onglet Référentiels du ministère : bouton « Direction régionale » sur chaque zone.
- `DirectionModal` est générique (direction d'établissement ou régionale) ; `SyntheseNationale` reçoit son chargeur en paramètre.

## Tests
`RegionServiceTest` (2 tests). `AdministrationServiceTest` inchangé.

## À essayer
Ministère → Référentiels → crée une zone, rattache-y un établissement (modifier), clique « Direction régionale », connecte-toi avec ce compte : il ne voit que cette zone, et reçoit 403 sur `POST /api/etablissements`.
