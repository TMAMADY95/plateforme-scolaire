# Étape 2 — Emplois du temps et salles (Module 9)

Cette étape ajoute les **emplois du temps** : salles d'un établissement, créneaux par classe
(matière + enseignant + salle + jour + horaire), **détection des conflits d'horaire**, et
consultation de l'emploi du temps par la direction, l'enseignant, l'élève et le parent.

---

## 1. Fichiers à copier (fusion directe)

Le dossier suit exactement l'arborescence du dépôt : **fusionne** son contenu dans la racine
de `plateforme-scolaire/`.

### Backend — nouveaux fichiers

| Fichier | Destination |
|---|---|
| `V2__emplois_du_temps.sql` | `backend/src/main/resources/db/migration/` |
| `domain/Salle.java` | `backend/src/main/java/com/plateforme/scolaire/domain/` |
| `domain/Creneau.java` | `backend/src/main/java/com/plateforme/scolaire/domain/` |
| `repository/SalleRepository.java` | `backend/src/main/java/com/plateforme/scolaire/repository/` |
| `repository/CreneauRepository.java` | `backend/src/main/java/com/plateforme/scolaire/repository/` |
| `service/EmploiDuTempsService.java` | `backend/src/main/java/com/plateforme/scolaire/service/` |
| `web/EmploiDuTempsController.java` | `backend/src/main/java/com/plateforme/scolaire/web/` |
| `web/dto/SalleDto.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `web/dto/SalleRequest.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `web/dto/CreneauDto.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `web/dto/CreneauRequest.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `web/dto/CreneauEleveDto.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `security/InscriptionPerimetreAdapter.java` | `backend/src/main/java/com/plateforme/scolaire/security/` |

> **Note sur `CreneauEleveDto.java`** : il est présent, mais l'endpoint élève réutilise en
> réalité `GET /api/classes/{classeId}/creneaux` (qui renvoie `CreneauDto`). Tu peux le
> supprimer si tu préfères garder le code minimal.

### Backend — fichiers à REMPLACER

| Fichier | Destination |
|---|---|
| `repository/InscriptionRepository.java` | `backend/src/main/java/com/plateforme/scolaire/repository/` |
| `security/PerimetreService.java` | `backend/src/main/java/com/plateforme/scolaire/security/` |

⚠️ **Ces deux fichiers ont été reconstruits à partir du contenu visible sur GitHub.** Avant de
les écraser, **compare-les à tes versions d'origine** : le concept global est identique, mais
vérifie que je n'ai pas perdu une méthode que tu avais ajoutée. Si tu vois une différence,
garde ta version et ajoute seulement les deux blocs marqués `// ---------- Emplois du temps ----------`
et `peutConsulterPresencesClasse` (que j'ai repris de ton code).

### Frontend — nouveaux fichiers

| Fichier | Destination |
|---|---|
| `api/emploisDuTemps.ts` | `frontend/src/api/` |
| `utils/horaires.ts` | `frontend/src/utils/` |
| `components/GrilleEmploiDuTemps.tsx` | `frontend/src/components/` |
| `pages/PageEmploisDuTemps.tsx` | `frontend/src/pages/` |
| `pages/PageEmploiDuTempsClasse.tsx` | `frontend/src/pages/` |
| `pages/enfant/OngletEmploiDuTemps.tsx` | `frontend/src/pages/enfant/` |

## 2. Éditions manuelles (petites)

### 2.1 `frontend/src/api/types.ts`

Copie le contenu de `frontend/src/types-emplois-du-temps.ts` à la fin de `types.ts`
(ce sont les interfaces `Salle`, `SalleRequete`, `Creneau`, `CreneauRequete`, `CreneauEleve`).

### 2.2 `frontend/src/App.tsx`

Ajoute les imports et deux routes :

```tsx
import PageEmploisDuTemps from './pages/PageEmploisDuTemps'
import PageEmploiDuTempsClasse from './pages/PageEmploiDuTempsClasse'
```

```tsx
{/* Emplois du temps — direction */}
<Route path="emplois-du-temps" element={<RequiertRole autorise={estGestionnaire}><PageEmploisDuTemps /></RequiertRole>} />
<Route
  path="emplois-du-temps/:classeId"
  element={<RequiertRole autorise={estGestionnaire}><PageEmploiDuTempsClasseWrapper /></RequiertRole>}
/>
```

Pour la route paramétrée, ajoute ce petit wrapper dans `App.tsx` (juste avant `export default`) :

```tsx
function PageEmploiDuTempsClasseWrapper() {
  const { classeId } = useParams()
  return <PageEmploiDuTempsClasse classeId={Number(classeId)} />
}
```

et l'import correspondant :

```tsx
import { useParams } from 'react-router'
```

### 2.3 Données de démonstration (optionnel mais recommandé)

Pour tester rapidement, ajoute un initialiseur qui crée 2 salles, un enseignant et 3 créneaux.
Sans cela, tu peux créer les salles et créneaux via Swagger UI ou curl (voir §3.3).

---

## 3. Tests

### 3.1 Migration

```bash
docker compose up -d
cd backend
mvn spring-boot:run
```

Flyway doit appliquer `V2__emplois_du_temps.sql`. Vérifie dans les logs :
`Migrating schema "public" to version "2 - emplois du temps"`.

> **Important** : la migration active l'extension PostgreSQL `btree_gist` (nécessaire pour les
> contraintes anti-chevauchement). L'utilisateur `scolaire` est propriétaire de la base, donc
> `CREATE EXTENSION` doit fonctionner. Si tu obtiens `permission denied to create extension`,
> exécute une fois en superutilisateur :
> ```bash
> docker exec -it scolaire-db psql -U postgres -d scolaire -c "CREATE EXTENSION IF NOT EXISTS btree_gist;"
> ```

### 3.2 Tests API (curl)

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"direction1@example.org","motDePasse":"Demo!2026"}' | jq -r .token)

# 1. Créer une salle
curl -s -X POST localhost:8080/api/etablissements/1/salles \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nom":"Salle 101","capacite":30,"typeSalle":"SALLE_DE_CLASSE"}' | jq

# 2. Lister les salles
curl -s localhost:8080/api/etablissements/1/salles -H "Authorization: Bearer $TOKEN" | jq

# 3. Créer un créneau (classe 1, matière 2, lundi 08:00-10:00)
curl -s -X POST localhost:8080/api/classes/1/creneaux \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"matiereId":2,"jourSemaine":1,"heureDebut":"08:00","heureFin":"10:00","salleId":1}' | jq

# 4. Conflit attendu (même salle, même jour, 09:00-11:00) -> 409
curl -si -X POST localhost:8080/api/classes/1/creneaux \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"matiereId":1,"jourSemaine":1,"heureDebut":"09:00","heureFin":"11:00","salleId":1}'
```

**Vérifications attendues :**
- Étape 3 → `201` avec le créneau créé.
- Étape 4 → **`409`** avec un message de conflit de salle.
- Un créneau **08:00-10:00** puis un créneau **10:00-12:00** dans la même salle le même jour
  doivent **passer** (`201`) : les intervalles se touchent sans se chevaucher.

### 3.3 Test de sécurité

- En `direction1@example.org`, `GET /api/etablissements/1/creneaux?anneeScolaireId=1` → `200`.
- En `direction1@example.org`, `GET /api/etablissements/2/salles` → **`403`** (hors périmètre).
- En `eleve1@example.org` (Étape 1), `GET /api/classes/1/creneaux` → `200` si l'élève est inscrit
  dans cette classe, **`403`** sinon.

### 3.4 Test interface

1. `cd frontend && npm run dev`.
2. Connecte-toi en `direction1@example.org` → nouvelle entrée **Emplois du temps**.
3. Tu dois voir les créneaux groupés par classe, et la liste des salles.

---

## 4. Limites connues de cette étape

- **Salles et créneaux se créent via API/Swagger**, pas encore via un formulaire dans l'interface.
  L'ajout d'un formulaire (créer/modifier un créneau) est une suite naturelle.
- Le module **9 du document** mentionne aussi les surveillants, les convocations d'examen et les
  remplacements : ce sont des sous-fonctionnalités de l'Étape 2bis, à faire plus tard.
- La contrainte anti-chevauchement est appliquée **deux fois** : en SQL (exclusion) et dans le
  service (message d'erreur lisible). C'est volontaire : la base garantit l'intégrité en dernier
  recours, le service explique le conflit à l'utilisateur.

---

## 5. Prochaine étape

**Étape 3 — Frais scolaires et paiements** (Module 7) : frais autorisés, factures par élève,
soldes, paiements via prestataire agréé, reçus numérotés.
