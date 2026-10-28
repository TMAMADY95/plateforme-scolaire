# Étape 1 — Espace élève : guide d'intégration et de test

Cette étape ajoute l'**espace élève** (Module 8 du document de cadrage) : un élève peut se
connecter, voir son profil (identité + classe), son bulletin et ses présences.

Le backend réutilise les endpoints déjà existants `GET /api/eleves/{id}/bulletin` et
`GET /api/eleves/{id}/presences` (déjà protégés par `@perimetre.peutAccederEleve`, qui autorise
un élève à accéder à son propre dossier). On ajoute seulement le point d'entrée
`GET /api/me/eleve` qui renvoie le dossier de l'élève connecté.

---

## 1. Fichiers à copier (nouveaux)

Copie chaque fichier à l'emplacement indiqué dans ton dépôt.

| Fichier fourni | À placer dans |
|---|---|
| `backend/.../web/dto/MonProfilEleveDto.java` | `backend/src/main/java/com/plateforme/scolaire/web/dto/` |
| `backend/.../dev/DevEleveInitializer.java` | `backend/src/main/java/com/plateforme/scolaire/dev/` |
| `frontend/src/hooks/useMonProfilEleve.ts` | `frontend/src/hooks/` |
| `frontend/src/pages/PageMonEspace.tsx` | `frontend/src/pages/` |

## 2. Fichiers à remplacer (versions complètes fournies)

Ces fichiers sont fournis en version complète : remplace les tiens.

| Fichier fourni | Remplace |
|---|---|
| `backend/.../repository/EleveRepository.java` | `backend/src/main/java/com/plateforme/scolaire/repository/EleveRepository.java` |
| `backend/.../service/EleveService.java` | `backend/src/main/java/com/plateforme/scolaire/service/EleveService.java` |
| `backend/.../web/MeController.java` | `backend/src/main/java/com/plateforme/scolaire/web/MeController.java` |
| `frontend/src/auth/roles.ts` | `frontend/src/auth/roles.ts` |

## 3. Éditions manuelles (petites, à faire toi-même)

### 3.1 `frontend/src/api/types.ts`

Ajoute cette interface (par exemple juste après `Enfant`) :

```typescript
export interface MonProfilEleve {
  eleveId: number
  matricule: string
  nom: string
  prenom: string
  dateNaissance: string
  sexe: string | null
  classeId: number | null
  classeNom: string | null
  etablissementId: number | null
  etablissementNom: string | null
  anneeScolaireId: number | null
  anneeLibelle: string | null
}
```

### 3.2 `frontend/src/api/endpoints.ts`

1. Ajoute `MonProfilEleve` à la liste des imports `type` en haut du fichier.
2. Ajoute cette fonction (par exemple dans la section « Espace parent ») :

```typescript
export const monProfilEleve = () => api.get<MonProfilEleve>('/api/me/eleve')
```

### 3.3 `frontend/src/App.tsx`

1. Ajoute l'import :

```typescript
import PageMonEspace from './pages/PageMonEspace'
```

2. Ajoute `estEleve` à l'import de `./auth/roles` :

```typescript
import { estEleve, estEnseignant, estGestionnaire, estParent } from './auth/roles'
```

3. Ajoute la route, après le bloc « Parents » :

```tsx
{/* Élèves */}
<Route path="mon-espace" element={<RequiertRole autorise={estEleve}><PageMonEspace /></RequiertRole>} />
```

### 3.4 Ordre des initialiseurs (backend)

Le nouveau `DevEleveInitializer` doit s'exécuter **après** les deux autres. Ajoute `@Order` :

- Dans `DevReferentielsInitializer.java` : ajoute `@Order(1)` sur la classe et l'import
  `import org.springframework.core.annotation.Order;`
- Dans `DevDataInitializer.java` : ajoute `@Order(2)` sur la classe et le même import.

Sans cela, le compte élève ne sera pas créé (un message d'avertissement apparaîtra dans les logs).

---

## 4. Tests

### 4.1 Démarrage

```bash
docker compose up -d          # PostgreSQL
cd backend
mvn test                      # tests unitaires existants (JWT)
mvn spring-boot:run           # API sur http://localhost:8080
```

Au premier démarrage, les logs doivent afficher :
`Compte élève de démonstration créé : eleve1@example.org (classe 6e A)`.

### 4.2 Test API (curl)

```bash
# 1. Connexion élève
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"eleve1@example.org","motDePasse":"Demo!2026"}' | jq -r .token)

# 2. Mon profil (doit renvoyer le dossier élève + classe 6e A)
curl -s localhost:8080/api/me/eleve -H "Authorization: Bearer $TOKEN"

# 3. Mon bulletin (remplacer ELEVE_ID par l'eleveId renvoyé, et PERIODE_ID par 1)
curl -s "localhost:8080/api/eleves/ELEVE_ID/bulletin?periodeId=1" -H "Authorization: Bearer $TOKEN"

# 4. Mes présences
curl -s "localhost:8080/api/eleves/ELEVE_ID/presences?du=2026-09-01&au=2026-10-28" \
  -H "Authorization: Bearer $TOKEN"
```

**Vérifications attendues :**
- Étape 2 → `200` avec `matricule: "ELEVE-DEMO-1"`, `classeNom: "6e A"`.
- Étape 3 → `200` (bulletin vide ou avec les notes saisies).
- Étape 4 → `200` (liste vide ou avec les présences saisies).

**Test de sécurité (important) :** connecte-toi avec `direction1@example.org` puis appelle
`GET /api/me/eleve` → tu dois recevoir `403` (le rôle DIRECTION n'a pas accès à cet endpoint).

### 4.3 Test interface (navigateur)

1. Lance le frontend : `cd frontend && npm install && npm run dev`.
2. Connecte-toi avec `eleve1@example.org` / `Demo!2026`.
3. Tu dois voir la page **« Mon espace »** avec ton nom, ta classe, et deux onglets
   **Bulletin** et **Présences**.

---

## 5. Prochaines étapes (feuille de route)

1. **Étape 2 — Emplois du temps** (Module 9) : entités `EmploiDuTemps`, `Salle`, `Creneau` + écran.
2. **Étape 3 — Frais & paiements** (Module 7) : factures, paiements, reçus.
3. **Étape 4 — Notifications & messagerie** (Module 10).
4. **Étape 5 — Dashboard ministère** (Module 1) : indicateurs nationaux, carte scolaire.

Dis-moi quand l'Étape 1 est validée (ou si un test échoue) et on enchaîne sur l'Étape 2.
