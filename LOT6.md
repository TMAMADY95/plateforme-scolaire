# Lot 6 — Administration nationale (ministère)

À fusionner à la racine, **après** `correctifs-revue-etapes-2-3a.zip` (les fichiers `endpoints.ts` et `types.ts` contiennent les deux livraisons).
Aucune migration : les tables existent déjà.

## Backend (rôle `MINISTERE` uniquement, vérifié côté serveur)
| Route | Rôle |
|---|---|
| `GET /api/zones` (aussi DIRECTION_REGIONALE) · `POST /api/zones` | zones géographiques |
| `POST /api/etablissements` | créer (code officiel unique, mis en majuscules, non modifiable ensuite) |
| `PUT /api/etablissements/{id}` | modifier |
| `POST /api/etablissements/{id}/statut` | ACTIF / SUSPENDU / FERME |
| `POST /api/etablissements/{id}/direction` | créer le compte de direction (changement de mot de passe forcé) |
| `GET /api/ministere/synthese?anneeScolaireId=` | établissements, classes, élèves inscrits |

Le ministère crée la structure mais n'écrit toujours pas dans les données internes d'un établissement. Chaque action est journalisée (`journal_audit`).
`EtablissementDto` gagne `adresse` et `capacite`.

## Front
Page `/ministere` (menu « Administration nationale », ouverte par défaut à la connexion du ministère) : chiffres clés, établissements (créer, modifier, statut, compte de direction), référentiels (années, niveaux, matières, zones).

## À tester
Connecte-toi avec le compte ministère de démo, crée une zone, un établissement, puis son compte de direction, reconnecte-toi avec ce compte (changement de mot de passe), et vérifie qu'un compte direction reçoit 403 sur `POST /api/etablissements`.

## Limites connues
- Pas de modification ni suppression de zones, ni de vue de la direction régionale (lot suivant).

## Complément : suspension effective
Un établissement SUSPENDU ou FERME retire les droits liés à lui. Un compte dont tous les rôles sont dans de tels établissements est désactivé : connexion refusée (même message qu'un mauvais mot de passe) et jetons déjà émis rejetés dès la requête suivante. Un compte qui a aussi un autre rôle (ex. parent) garde celui-là. Réactiver l'établissement rend l'accès. Fichiers : `CustomUserDetailsService.java`, `CustomUserDetailsServiceTest.java`.
