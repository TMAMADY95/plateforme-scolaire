# Lot 10 — comptes, audit, zones

Copier `backend/` et `frontend/` par-dessus le projet (aucune suppression, aucune migration SQL).

## Réinitialisation de mot de passe par un administrateur (pas d'e-mail)
- `POST /api/utilisateurs/{id}/reinitialisation-mot-de-passe` : fixe un mot de passe temporaire (10 caractères minimum), force le changement à la prochaine connexion, invalide les jetons déjà émis, lève le verrouillage. Le mot de passe n'est jamais écrit dans le journal d'audit.
- Qui peut réinitialiser qui :
  - Ministère : directions d'établissement et directions régionales.
  - Direction d'un établissement : son personnel administratif, ses enseignants, et les parents et élèves inscrits chez elle.
  - Personne ne réinitialise son propre mot de passe par cette voie, ni un compte qui cumule un rôle hors de son périmètre.
- Écrans : page « Comptes » (direction), et « Comptes de direction » dans le menu d'un établissement (ministère). Le mot de passe temporaire est généré, affiché une seule fois, avec un bouton Copier.

## Personnel administratif
- `POST /api/etablissements/{id}/personnel` (direction uniquement) et bouton « Nouveau personnel administratif ».

## Journal d'audit
- `GET /api/audit?action=&entite=&page=&taille=` (ministère uniquement), onglet « Journal d'audit » : filtres, pagination de 25 entrées, de la plus récente à la plus ancienne.

## Zones
- `PUT /api/zones/{id}` : renommer ou déplacer, sans jamais créer de cycle.
- `DELETE /api/zones/{id}` : refusé (409) s'il y a des sous-zones, des établissements ou une direction régionale.

## Tests ajoutés
PerimetreComptesServiceTest, ComptesServiceTest, ConsultationAuditServiceTest, AdministrationZonesTest.

## À faire de ton côté
`mvn clean verify` puis `npm run build`. Rien n'est compilé de mon côté (réseau bloqué) : seule la syntaxe a été vérifiée.
