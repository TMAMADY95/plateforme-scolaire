# Plateforme de gestion scolaire

Monorepo : `backend/` (Spring Boot, PostgreSQL), `web/` (React, à créer), `mobile/` (Flutter, à créer).

## Prérequis

- JDK 21 ou plus récent
- Maven 3.9+
- Docker (pour PostgreSQL)

## Démarrage

```bash
docker compose up -d                 # PostgreSQL sur localhost:5432
cd backend
mvn test                             # tests unitaires (JWT)
mvn spring-boot:run                  # API sur http://localhost:8080
```

Au premier démarrage, Flyway applique `V1__schema_initial.sql`. Avec le profil `dev` (par défaut),
des comptes de démonstration sont créés :

| Compte                  | Rôle                          | Périmètre                       |
| ----------------------- | ----------------------------- | ------------------------------- |
| `ministere@example.org` | MINISTERE                     | National (tous les établissements) |
| `direction1@example.org`| DIRECTION_ETABLISSEMENT       | École démo 1 uniquement         |

Mot de passe de démonstration : `Demo!2026` (variable `DEV_PASSWORD`).

## Tester l'API

Documentation interactive : http://localhost:8080/swagger-ui.html

```bash
# 1. Connexion
curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"direction1@example.org","motDePasse":"Demo!2026"}'

# 2. Liste des établissements (remplacer TOKEN) : la direction n'en voit qu'un
curl -s localhost:8080/api/etablissements -H 'Authorization: Bearer TOKEN'

# 3. Accès à un établissement hors périmètre : 403 attendu
curl -si localhost:8080/api/etablissements/2 -H 'Authorization: Bearer TOKEN'
```

Avec `ministere@example.org`, la liste contient les deux établissements.

## Architecture des permissions

- `affectation_role` associe un utilisateur, un rôle et un périmètre (NATIONAL, ZONE, ETABLISSEMENT, PERSONNEL).
- `PerimetreService` décide si l'utilisateur connecté peut accéder à un établissement. Il s'utilise dans
  les contrôleurs : `@PreAuthorize("@perimetre.peutAccederEtablissement(#id)")`.
- L'`ADMIN_TECHNIQUE` n'a jamais d'accès automatique aux données des établissements.
- Les listes sont filtrées côté serveur (`EtablissementService.listerAccessibles`).

## Configuration

| Variable                  | Rôle                                         | Défaut (dev)            |
| ------------------------- | -------------------------------------------- | ----------------------- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL                | localhost / scolaire    |
| `JWT_SECRET`              | Clé de signature (32 caractères minimum)     | valeur de dev           |
| `CORS_ORIGINS`            | Origines autorisées (React, Flutter web)     | localhost:5173, :3000   |
| `SPRING_PROFILES_ACTIVE`  | `dev` ou `prod`                              | `dev`                   |

En `prod` : pas de comptes de démonstration, Swagger désactivé, `JWT_SECRET` obligatoire.

## À faire ensuite

- Essayer `spring.jpa.hibernate.ddl-auto: validate` pour vérifier que les entités correspondent au schéma.
- Endpoints métier : classes, élèves, inscriptions, présences, notes, avec la vérification de périmètre.
- Limitation des tentatives de connexion, récupération de mot de passe, journalisation dans `journal_audit`.
- Tests d'intégration avec Testcontainers (PostgreSQL).
- Migrer vers Spring Boot 4.1 une fois le projet stabilisé (la ligne 4.0 est maintenue jusqu'à fin 2026).
"# plateforme-scolaire" 
