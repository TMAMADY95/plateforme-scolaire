# Jeu de données de démonstration

Un réseau scolaire de Guinée complet pour tester tous les scénarios de la plateforme : **5 régions, 29 établissements, ≈ 4 750 élèves,
≈ 4 450 comptes, ≈ 146 000 notes, ≈ 70 000 présences, ≈ 11 000 factures, 9 examens avec centres et candidats.**

> Tous les comptes ont le mot de passe **`Demo!2026`** (le même que les comptes `*@example.org` du profil dev).
> Les données sont fictives. Le script s'exécute en une dizaine de secondes.

## Charger les données

1. Démarrez le backend une première fois (les migrations Flyway, jusqu'à V36, créent les tables).
2. Chargez le script, **une seule fois** :

```bash
# Avec le conteneur Docker du projet (fonctionne aussi sous Windows)
docker cp backend/donnees-demo/demo_donnees.sql scolaire-db:/tmp/demo_donnees.sql
docker exec -e PGCLIENTENCODING=UTF8 scolaire-db psql -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f /tmp/demo_donnees.sql

# Ou avec psql installé sur la machine
psql -h localhost -p 5433 -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f backend/donnees-demo/demo_donnees.sql
```

Le script ajoute des données sans toucher à vos classes, salles, factures ou comptes existants (seuls deux réglages de référence sont ajustés : le niveau des types d'examen CEE, BEPC et BAC s'il n'était pas renseigné, et les conditions pour être centre d'examen, réglées sur « autorisé, 3 salles, 60 places ») et s'arrête avec un message s'il a déjà été chargé.
Il est exécuté dans une transaction : en cas d'erreur, rien n'est enregistré.

**Tout supprimer** (établissements `GN-xxxx`, comptes `@demo.gn`, élèves `DEMO-xxxxxx`, zones « Guinée », examens de démonstration) :

```bash
docker cp backend/donnees-demo/supprimer_donnees_demo.sql scolaire-db:/tmp/supprimer_donnees_demo.sql
docker exec -e PGCLIENTENCODING=UTF8 scolaire-db psql -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f /tmp/supprimer_donnees_demo.sql
```
(Les années, niveaux `GN-xx` et matières créés par le script restent en place. Si vous avez ajouté vous-même des données liées à ces comptes,
la suppression peut être refusée : dans ce cas, recréez la base.)

## Comptes

| Rôle | Comptes | Périmètre |
|---|---|---|
| Administrateur technique | `admin@demo.gn` | national |
| Ministère | `ministere.demo@demo.gn`, `secretaire.general@demo.gn` (et `ministere@example.org` du profil dev) | national |
| Direction régionale | `dr.conakry@demo.gn`, `dr.kindia@demo.gn`, `dr.labe@demo.gn`, `dr.kankan@demo.gn`, `dr.nzerekore@demo.gn` | une région |
| Direction préfectorale | `dp.coyah@demo.gn`, `dp.kindia@demo.gn` | une préfecture |
| Direction communale | `dc.coyah@demo.gn` (Coyah-centre), `dc.ratoma@demo.gn` (Ratoma) | une commune |
| Direction d'établissement | `dir.0001@demo.gn` … `dir.0029@demo.gn` (le numéro est celui du code `GN-0001` … `GN-0029`) | un établissement |
| Secrétariat / comptabilité | `sec.0001@demo.gn` … `sec.0029@demo.gn` | un établissement |
| Enseignants | `ens.0003.01@demo.gn`, `ens.0003.02@demo.gn`… (établissement puis numéro : un enseignant par matière et par école) | un établissement |
| Parents | `parent.1@demo.gn`, `parent.2@demo.gn`… (≈ 4 000 comptes, 85 % avec téléphone) | leurs enfants |
| Élèves | `eleve.<numéro>@demo.gn` (un élève de collège ou de lycée sur douze ; voir la requête plus bas) | eux-mêmes |

## Les 29 établissements

| Zone | Établissements (code) |
|---|---|
| Conakry : Kaloum | GN-0001 École primaire Kaloum 1 · GN-0002 Collège Kaloum |
| Conakry : Dixinn | GN-0003 Lycée Donka · GN-0004 École Sainte-Marie (privé, confessionnel) |
| Conakry : Matam | GN-0005 Groupe scolaire Matam |
| Conakry : Ratoma | GN-0006 Lycée Ratoma 1 · GN-0007 Complexe Les Étoiles (privé, 3 cycles) · GN-0008 École franco-arabe Al Nour (privé) |
| Conakry : Matoto | GN-0009 Collège Matoto · GN-0010 Lycée technique (technique) |
| Kindia : Coyah-centre | GN-0011 École primaire · GN-0012 Collège · GN-0013 Lycée · GN-0014 École communautaire de Manéah · GN-0015 Collège privé Les Cèdres |
| Kindia : Kindia-centre / Dubréka-centre | GN-0016 Collège Kindia · GN-0017 Lycée Kindia · GN-0018 École primaire Dubréka |
| Labé | GN-0019 Lycée · GN-0020 Collège · GN-0021 École privée Fouta · GN-0022 École communautaire de Koubia |
| Kankan | GN-0023 Lycée · GN-0024 Collège · GN-0025 École primaire · GN-0026 Collège Siguiri (suspendu) |
| Nzérékoré | GN-0027 Lycée · GN-0028 École primaire Nzérékoré 2 (fermée) · GN-0029 Collège Yomou |

## Scénarios couverts et où les tester

- **Périmètres et hiérarchie** : `dr.*` ne voit que sa région ; `dp.*` et `dc.*` (préfecture, commune) ne voient que leur zone. Le ministère voit tout.
- **Établissements** : secteurs public/privé, 5 types (classique, communautaire, franco-arabe, confessionnel, technique), statuts actif/suspendu/fermé,
  autorisations (autorisée, en attente : GN-0014, GN-0015, GN-0022 ; non autorisée : GN-0026 ; retirée : GN-0028). Filtres et statistiques par type.
- **Élèves au niveau national** (ministère) : ≈ 4 750 élèves à filtrer par zone, secteur, type, cycle, niveau, sexe, statut, avec export PDF.
- **Inscriptions** : ≈ 50 demandes **en attente** à décider avec les comptes `dir.xxxx`, des refusées, des annulées et des **transferts**.
  Les deux années 2025-2026 et 2026-2027 contiennent des classes et des inscriptions.
- **Pédagogie** : classes (1ère année à Terminale), un enseignant par matière, coefficients, **emplois du temps sans conflit** (salles et enseignants),
  évaluations et notes de la 1re période de 2026-2027 et des périodes complètes de 2025-2026. Les écoles privées, franco-arabes et techniques
  fonctionnent en **semestres** ; les autres en **trimestres** (utile pour la réussite du réseau par période).
- **Vie scolaire** : ≈ 70 000 présences (12 au 30 octobre 2026), élèves très absents, **justifications d'absence** (en attente, acceptées, refusées),
  devoirs (faits ou non), annonces de classe, cahier de textes, rendez-vous parents-enseignants, autorisations de sortie, messagerie.
- **SMS d'absence aux parents** : activez `SMS_ABSENCES=true`, puis saisissez une absence avec un compte direction : les parents qui ont un téléphone
  sont prévenus.
- **Finances** (en GNF) : frais d'inscription, de scolarité (par niveau), cantine, tenue ; ≈ 11 000 factures impayées, partielles, payées,
  exonérées (bourses, remises, aides sociales) ou annulées ; paiements en espèces, virement, chèque et mobile money, y compris **en attente, en échec
  et annulés**. Les numéros de facture sont répartis par plages (`FAC-2026-1xxxxx` pour GN-0001, `2xxxxx` pour GN-0002…).
- **Conformité** : ≈ 50 inspections avec pièces jointes, signalements de sécurité ouverts et résolus, plans d'action (certains en retard, avec rappels),
  alertes d'urgence. Cas particuliers : GN-0009 (signalement grave ouvert, donc **urgent**), GN-0014 et GN-0015 (**jamais inspectés**), GN-0019
  (inspection de **plus d'un an**), GN-0026 et GN-0028 (non conformes).
- **Postes et mutations d'enseignants** : chaque école a un nombre de postes légèrement supérieur ou inférieur à ses enseignants en poste ;
  la mutation n'est possible qu'entre établissements **publics**.
- **Examens** (2025-2026 avec résultats ; 2026-2027 en préparation) :
  - *CEE 2026*, *BEPC 2026*, *Baccalauréat 2026* : résultats transmis par 85 % des établissements concernés (les autres sont « sans résultats »).
  - *Évaluation de rentrée 6e année* (octobre 2026, organisée par la préfecture) : épreuves passées, candidats avec centre, résultats transmis par la moitié
    des écoles seulement : les autres reçoivent les **rappels de résultats non transmis** (le planificateur tourne à 07 h).
  - *CEE 2027* (préfecture), *BEPC 2027* (région), *Baccalauréat 2027* (**national**), *Évaluation communale 6e année* (**commune**) :
    période, emploi du temps, centres désignés, candidats inscrits (≈ 88 % des élèves de la classe d'examen), environ 70 % déjà affectés à un centre.
  - *CEE franco-arabe 2027* : réservé aux **établissements franco-arabes** (types participants).
- **Statistiques détaillées** : les résultats comptent des absents et des éliminés pour fraude ; 80 % des établissements ont renseigné les filles présentes et admises ; le Baccalauréat 2026 est réparti par série. Ouvrez Résultats d'un examen (ministère) pour voir les taux par sexe, région, préfecture et série.
- **Jury et proclamation** : le CEE 2026 est proclamé (saisie close), le BEPC 2026 est délibéré et attend la proclamation du ministère, le Baccalauréat 2026 a son jury constitué (7 membres présents sur 9, quorum atteint) mais n'a pas encore délibéré. Ouvrez Résultats, puis « Jury et proclamation ».
- **Centres d'examen** : conditions réglées sur « autorisé, au moins 3 salles et 60 places ». Un centre par commune est déjà désigné. L'école de Koubia
  (GN-0022) n'est pas éligible (autorisation en attente) : la commune n'a donc **aucun centre** et ses candidats apparaissent « sans centre dans leur commune »
  lors de l'**affectation automatique**. Relancez l'affectation pour voir les candidats restants (≈ 30 %) rejoindre le centre de leur commune.
- **Journal d'audit** : un échantillon d'événements.

## Requêtes utiles pour trouver des exemples

Les numéros de comptes parents et élèves sont stables, mais certains exemples dépendent des données : ces requêtes les retrouvent.

```sql
-- Parents dont les enfants sont dans deux écoles différentes
SELECT u.email, string_agg(DISTINCT e.code_officiel, ', ')
FROM lien_parental l JOIN utilisateur u ON u.id = l.utilisateur_id
JOIN inscription i ON i.eleve_id = l.eleve_id AND i.statut = 'ACCEPTEE'
JOIN annee_scolaire a ON a.id = i.annee_scolaire_id AND a.libelle = '2026-2027'
JOIN etablissement e ON e.id = i.etablissement_id
WHERE u.email LIKE '%@demo.gn' GROUP BY u.email HAVING count(DISTINCT e.id) > 1 LIMIT 10;

-- Comptes élèves
SELECT u.email, e.nom, e.prenom FROM eleve e JOIN utilisateur u ON u.id = e.utilisateur_id WHERE u.email LIKE '%@demo.gn' LIMIT 10;

-- Élèves très absents dont le parent a un téléphone
SELECT el.matricule, u.email, u.telephone, count(*) AS absences
FROM presence p JOIN inscription i ON i.id = p.inscription_id JOIN eleve el ON el.id = i.eleve_id
JOIN lien_parental l ON l.eleve_id = el.id AND l.responsable_legal JOIN utilisateur u ON u.id = l.utilisateur_id
WHERE p.statut = 'ABSENT' AND u.telephone IS NOT NULL GROUP BY 1, 2, 3 ORDER BY absences DESC LIMIT 10;

-- Écoles qui n'ont pas transmis les résultats de l'évaluation de rentrée
SELECT DISTINCT e.code_officiel, e.nom
FROM candidat_examen c JOIN examen_national x ON x.id = c.examen_id AND x.libelle LIKE 'Évaluation de rentrée%'
JOIN etablissement e ON e.id = c.etablissement_id
WHERE NOT EXISTS (SELECT 1 FROM resultat_examen r WHERE r.examen_id = c.examen_id AND r.etablissement_id = c.etablissement_id);

-- Candidats sans centre pour le CEE 2027
SELECT e.code_officiel, e.nom, count(*) FROM candidat_examen c
JOIN examen_national x ON x.id = c.examen_id AND x.libelle = 'CEE 2027' JOIN etablissement e ON e.id = c.etablissement_id
WHERE c.centre_id IS NULL GROUP BY 1, 2 ORDER BY 3 DESC;
```

## À savoir

- Le script a été vérifié sur un PostgreSQL 16 vierge (migrations V1 à V36, base UTF-8) : exécution sans erreur, contraintes et index respectés,
  suppression complète testée. Il n'a pas été essayé avec l'application elle-même.
- Les notes sont indicatives (moyenne autour de 10,6 sur 20) ; les montants sont en francs guinéens (GNF).
- La date « du jour » des données est le **31 octobre 2026** : les présences, devoirs et rendez-vous s'étalent autour de cette date.
- Anomalie existante repérée en préparant les factures : l'index `ux_facture_numero` impose l'unicité du numéro sur **toute la base**, alors que
  l'application numérote par établissement (`FAC-2026-000001` pour chaque école). Dès que deux écoles émettront leur première facture par l'application,
  la seconde sera refusée. Le jeu de données évite le problème avec des plages distinctes par école.
