# Étape 3a — Frais scolaires et factures (Module 7, partie 1)

Cette étape met en place la **grille tarifaire**, l'**émission des factures** et le **calcul des soldes**.
Elle ne traite **pas encore les paiements** : c'est la 3b, avec l'intégration Orange Money.

---

## 1. La migration

Fichier : `backend/src/main/resources/db/migration/V5__frais_et_factures.sql`

> ⚠️ **Vérifie le numéro** : ton dossier contient `V1` à `V4`. Si tu as ajouté une migration
> depuis, renomme celle-ci en `V6` avant de lancer.

Elle crée 5 tables : `frais`, `facture`, `exoneration`, `compteur_facture`, et les index associés.
**Aucune table de paiement** — volontairement.

### Deux règles structurantes dans le code

- **Le montant dû est figé à l'émission.** Modifier la grille tarifaire ensuite ne réécrit pas
  les factures déjà émises. C'est ce qu'attend un service financier.
- **La génération est idempotente.** Relancer la génération crée les factures manquantes et
  ignore celles qui existent. Jamais de doublon, même si on clique deux fois.

---

## 2. Fichiers à copier

Le dossier suit l'arborescence du dépôt : **fusionne** son contenu dans la racine de
`plateforme-scolaire/`.

### Backend — nouveaux fichiers (18)

| Fichier | Destination |
|---|---|
| `V5__frais_et_factures.sql` | `backend/src/main/resources/db/migration/` |
| `domain/Frais.java` | `.../domain/` |
| `domain/Facture.java` | `.../domain/` |
| `domain/Exoneration.java` | `.../domain/` |
| `repository/FraisRepository.java` | `.../repository/` |
| `repository/FactureRepository.java` | `.../repository/` |
| `repository/ExonerationRepository.java` | `.../repository/` |
| `service/FraisService.java` | `.../service/` |
| `service/FacturationService.java` | `.../service/` |
| `service/MesFacturesService.java` | `.../service/` |
| `security/PerimetreFinancesService.java` | `.../security/` |
| `web/FacturationController.java` | `.../web/` |
| `web/dto/FraisDto.java` | `.../web/dto/` |
| `web/dto/FraisRequest.java` | `.../web/dto/` |
| `web/dto/FactureDto.java` | `.../web/dto/` |
| `web/dto/ExonerationRequest.java` | `.../web/dto/` |
| `web/dto/GenererFacturesRequest.java` | `.../web/dto/` |
| `web/dto/ResultatGenerationDto.java` | `.../web/dto/` |
| `web/dto/BilanFinancierDto.java` | `.../web/dto/` |

> ✅ **Aucun fichier existant n'est remplacé.** `PerimetreService` garde ses 6 dépendances
> d'origine : tes 104 tests restent valides sans modification. C'est tout l'intérêt d'avoir
> sorti les règles financières dans `PerimetreFinancesService`.

### Frontend — nouveaux fichiers (4)

| Fichier | Destination |
|---|---|
| `api/facturation.ts` | `frontend/src/api/` |
| `utils/monnaie.ts` | `frontend/src/utils/` |
| `pages/PageFinances.tsx` | `frontend/src/pages/` |
| `pages/enfant/OngletFactures.tsx` | `frontend/src/pages/enfant/` |

## 3. Éditions manuelles (3 petites)

### 3.1 `frontend/src/api/types.ts`

Copie le contenu de `frontend/src/types-facturation.ts` à la fin du fichier
(`Frais`, `FraisRequete`, `StatutFacture`, `Facture`, `GenererFacturesRequete`,
`ResultatGeneration`, `ExonerationRequete`, `BilanFinancier`).

### 3.2 `frontend/src/App.tsx`

```tsx
import PageFinances from './pages/PageFinances'
```

```tsx
{/* Finances — direction */}
<Route path="finances" element={<RequiertRole autorise={estGestionnaire}><PageFinances /></RequiertRole>} />
```

### 3.3 Ajouter l'onglet « Frais » dans les espaces élève et parent

Dans `OngletBulletin` / `OngletPresences` de l'espace parent (`PageEnfant.tsx`), ajoute un
troisième onglet :

```tsx
import OngletFactures from './enfant/OngletFactures'

<Tabs.Tab value="frais">Frais</Tabs.Tab>
...
<Tabs.Panel value="frais" pt="md">
  <OngletFactures eleveId={enfant.eleveId} />
</Tabs.Panel>
```

Et dans `PageMonEspace.tsx` (espace élève, Étape 1) :

```tsx
<Tabs.Tab value="frais">Frais</Tabs.Tab>
...
<Tabs.Panel value="frais" pt="md">
  <OngletFactures eleveId={null} />
</Tabs.Panel>
```

---

## 4. Tests

### 4.1 Démarrage

```bash
docker compose up -d
cd backend
mvn -q clean test      # tes 104 tests doivent toujours passer
mvn spring-boot:run
```

### 4.2 Créer une grille tarifaire

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"direction1@example.org","motDePasse":"Demo!2026"}' | jq -r .token)

# Frais « Frais de scolarité » : 25 000, pour tous les niveaux
curl -s -X POST localhost:8080/api/etablissements/1/frais \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"anneeScolaireId":1,"libelle":"Frais de scolarité","montant":25000,"devise":"XOF","echeance":"2026-11-30","obligatoire":true}' | jq

# Frais « Cantine » : 8 000
curl -s -X POST localhost:8080/api/etablissements/1/frais \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"anneeScolaireId":1,"libelle":"Cantine","montant":8000,"devise":"XOF","obligatoire":true}' | jq
```

**Attendu** : `201` avec un `id` et `actif: true` pour chacun.

**Test d'unicité** : relance la première commande → **`409`** (« Un frais portant ce libellé existe déjà »).

### 4.3 Émettre les factures

Il faut au préalable une classe avec des élèves inscrits. Avec les données de démonstration de
l'Étape 1, la classe `6e A` et son élève existent.

```bash
# Génération pour la classe 1
curl -s -X POST localhost:8080/api/etablissements/1/factures/generation \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"anneeScolaireId":1,"classeId":1}' | jq
```

**Attendu** : `201` avec `facturesCreees: 2`, `facturesIgnorees: 0`, `elevesTraites: 1`,
`montantTotalEmis: 33000`.

> Le nombre de factures créées = nombre d'élèves × nombre de frais obligatoires.
> Ajuste si tu as ajouté d'autres frais ou d'autres élèves.

**Test d'idempotence (le plus important)** : relance **exactement la même commande**.

**Attendu** : `201` avec **`facturesCreees: 0`** et **`facturesIgnorees: 2`**.
Aucun doublon n'est créé. C'est la garantie anti-double-facturation.

### 4.4 Consulter les factures et le bilan

```bash
# Toutes les factures
curl -s "localhost:8080/api/etablissements/1/factures?anneeScolaireId=1" -H "Authorization: Bearer $TOKEN" | jq

# Bilan financier
curl -s "localhost:8080/api/etablissements/1/factures/bilan?anneeScolaireId=1" -H "Authorization: Bearer $TOKEN" | jq
```

**Attendu au bilan** : `totalDu: 33000`, `totalPaye: 0`, `totalRestant: 33000`, `nombreImpayees: 2`.

### 4.5 Exonération (bourse)

```bash
# Remplacer FACTURE_ID par l'id d'une facture, et 1 par un id utilisateur valide
curl -s -X POST localhost:8080/api/factures/FACTURE_ID/exonerations \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"montant":5000,"motif":"Bourse sociale","typeExoneration":"BOURSE"}' | jq
```

**Attendu** : la facture renvoie `montantExonere: 5000` et `resteAPayer` diminué de 5 000.

**Test de borne** : tente une exonération de 30 000 sur une facture de 25 000 → **`400`**
(« Le total exonéré dépasse le montant de la facture »).

### 4.6 Test de sécurité

- `direction1@example.org` sur `GET /api/etablissements/2/frais?anneeScolaireId=1` → **`403`** (hors périmètre).
- `eleve1@example.org` (Étape 1) sur `GET /api/me/factures` → **`200`** avec ses propres factures.
- `eleve1@example.org` sur `GET /api/etablissements/1/factures?anneeScolaireId=1` → **`403`**.

---

## 5. Limites connues de la 3a

- **Pas de formulaire dans l'interface** pour créer un frais ou lancer la génération : cela se
  fait via Swagger UI ou curl. L'écran de saisie est prévu en 3c.
- **Aucun paiement** : la colonne « Payé » reste à 0 jusqu'à l'Étape 3b. Le calcul est déjà
  branché (`FacturationService.montantPaye`) et se mettra à jour tout seul dès que la table
  `paiement` existera.
- Les **statuts** `PARTIELLE` et `PAYEE` ne peuvent donc pas encore être observés.

---

## 6. Prochaine étape

**Étape 3b — Paiements et reçus** : encaissement (espèces, virement, mobile money), reçus
numérotés, et intégration **Orange Money** — avec vérification de la confirmation du
prestataire avant de marquer une facture payée, et **sans jamais stocker de données bancaires**.
