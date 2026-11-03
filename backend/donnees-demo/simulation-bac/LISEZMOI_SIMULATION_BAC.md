# Simulation pas à pas d'un baccalauréat

Neuf scripts SQL rejouent l'organisation d'un baccalauréat national, de la préparation à l'après-proclamation. Chaque script ajoute une étape
**par-dessus la précédente** : chargez-en un, regardez les écrans, puis chargez le suivant. L'examen s'appelle **« Baccalauréat simulé 2026 »**
(année scolaire **2025-2026**, 12e année, noté sur 20, du 29 juin au 3 juillet 2026). Il est indépendant du « Baccalauréat 2026 » du jeu de démonstration.
Toutes les données sont fictives.

## Prérequis

1. Backend démarré au moins une fois (migrations Flyway jusqu'à **V65**).
2. `demo_donnees.sql` puis `demo_examens_complements.sql` chargés (voir `../LISEZMOI_DONNEES_DEMO.md`). `demo_dgecs_complements*.sql` sont facultatifs.
3. Dans l'application, choisir l'année scolaire **2025-2026**.

## Charger

```powershell
# Windows PowerShell, depuis backend\donnees-demo\simulation-bac
.\charger_etapes.ps1 -De 1 -A 1      # étape 1 seule
.\charger_etapes.ps1 -De 2 -A 2      # puis l'étape 2, etc.
.\charger_etapes.ps1                 # ou tout d'un coup (étapes 1 à 9)
```
Sous Linux ou macOS : `./charger_etapes.sh 1 1`, `./charger_etapes.sh 2 2`, etc. Chaque étape est une transaction : en cas d'erreur, rien n'est enregistré pour elle.
Une étape refuse de se charger si la précédente manque ou si elle a déjà été chargée. **`supprimer_simulation_bac.sql`** supprime toute la simulation (quel que soit l'avancement).

## Comptes (mot de passe `Demo!2026`)

| Compte | À utiliser pour |
|---|---|
| `dgecs.demo@demo.gn` | Piloter l'examen : tous les onglets DGECS, saisies (besoins, budget, convois, réclamations, rectifications) |
| `ministere.demo@demo.gn` | Consulter, proclamer, voir les chiffres clés |
| `chef.bac1@demo.gn` … `chef.bac7@demo.gn` | Chef de centre : « Mon centre d'examen » (un compte par centre) |
| `dir.0003@demo.gn`, `dir.0006@demo.gn`, `dir.0007@demo.gn`, `dir.0010@demo.gn`, `dir.0013@demo.gn`, `dir.0017@demo.gn`, `dir.0019@demo.gn`, `dir.0023@demo.gn`, `dir.0027@demo.gn` | Direction d'un lycée candidat : candidats, listes, résultats de l'établissement |

Les 7 centres : Donka (1), Ratoma 1 (2), Coyah (3), Kindia (4), Labé (5), Kankan (6), Nzérékoré (7) ; le numéro est celui du compte `chef.bac<N>`.
Les candidats de GN-0010 vont au centre de Donka, ceux de GN-0007 au centre de Ratoma 1.

## Les neuf étapes

| # | Script | Période simulée | Ce qui se passe | Où le voir (compte DGECS sauf mention) |
|---|---|---|---|---|
| 1 | `bac_etape_01_preparation.sql` | janvier 2026 | L'examen est créé avec 7 épreuves (coefficients 2 à 4), un calendrier prévisionnel (correction 6-17 juillet, délibération 24, proclamation 31), des besoins en personnel globaux et par épreuve, un budget prévisionnel de 900 millions GNF (rien d'engagé). | Onglet **Examens** ; **Logistique des centres** (choisir l'examen) : « Personnel mobilisé » montre tout le personnel manquant et « Besoins en personnel par épreuve » le pic ; **Indicateurs et budget** : budget prévu. |
| 2 | `bac_etape_02_inscriptions.sql` | février à avril | 331 candidats (317 élèves de 12e année et 14 candidats libres) dans 9 lycées, avec leur série ; 5 candidats avec aménagement (handicap) ; dossiers avec ≈ 6 % de pièces manquantes (beaucoup à Labé) ; listes des établissements à des stades différents : 4 validées, 2 validées par la région, 2 transmises, 1 **renvoyée** pour dossiers incomplets (GN-0019). | **Candidats et résultats** ; onglet **Examens** (listes de candidats et dossiers) ; `dir.0019@demo.gn` voit le motif du renvoi ; **État des centres d'examen** (aménagements). |
| 3 | `bac_etape_03_centres.sql` | avril à mai | 7 centres désignés et caractérisés (Labé enclavé, Nzérékoré éloigné et inondable, toilettes à réparer…) ; 331 candidats affectés avec numéro de PV, salle (15 salles) et table ; dossiers régularisés, **toutes les listes validées** ; 93 membres du personnel des centres (une salle de Nzérékoré n'a qu'un surveillant) ; préparation des centres inachevée pour 3 d'entre eux ; 26 personnels nationaux (jury 9/9, superviseurs 8/12, inspecteurs 5/8, informatique 4/6) ; 7 comptes de chefs de centre. | **Logistique des centres** (centres prêts ou non, personnel), **État des centres d'examen** ; `chef.bac7@demo.gn` > **Mon centre d'examen** (préparation à terminer). |
| 4 | `bac_etape_04_sujets_logistique.sql` | mai à juin | 21 sujets (3 variantes par épreuve) validés ; 49 plis (un par centre et par épreuve, candidats + 5 %) ; fournitures livrées (Nzérékoré attend encore ses kits) ; 7 convois de transport, deux incidents (**Nzérékoré : 28 h de retard**, piste inondée ; **Kankan : rupture d'enveloppes**) ; préparation des centres terminée, personnel national complété ; premiers engagements de budget. | **Logistique des centres** : « Transport des sujets et du matériel » (retards, incidents), « Sujets à imprimer » (candidats + 5 %, 2 feuilles par candidat) ; **Indicateurs et budget**. |
| 5 | `bac_etape_05_epreuves.sql` | 29 juin au 3 juillet | Les plis sont ouverts avec deux témoins, les compositions ont lieu : 310 candidats présents, 18 absents, **3 éliminés pour fraude** ; 13 incidents (retards, malaises, coupure de courant, une erreur de table) dont une **fuite présumée du sujet de mathématiques** : variante A compromise, variante B tirée, épreuve débutée à 10 h au lieu de 8 h partout ; deux copies introuvables à Coyah en anglais ; à Nzérékoré le rapport d'histoire-géographie n'est pas clos. | **Participation** (présence, ponctualité : mathématiques « hors tolérance » dans les 7 centres, copies en rouge à Coyah en anglais) ; **Copies et fiabilité** (fraudes, incidents) ; `chef.bac2@demo.gn` > **Mon centre d'examen** (incidents, plis ouverts, rapports). |
| 6 | `bac_etape_06_correction.sql` | 6 au 17 juillet | 7 commissions de correction (72 correcteurs) ont corrigé toutes les copies ; 2 170 notes saisies ; moyennes pondérées et décisions : **140 admis sur 310 présents (45 %)**, moyenne générale 9,26/20 ; les éliminés sont non admis, les absents sans décision. | **Copies et fiabilité** (correction à 100 %, correcteurs) ; **Candidats et résultats** (moyennes, admis) ; **Logistique des centres** : correcteurs disponibles. |
| 7 | `bac_etape_07_deliberation.sql` | 24 juillet | Le jury (8 présents sur 9) délibère : l'examen est **délibéré**, pas encore proclamé ; résultats consolidés par établissement (filles comprises) et par série. | **Examens** > examen > « Jury et proclamation » (le ministère peut proclamer, mais l'étape 8 le fait déjà) ; **Statistiques des examens** (taux par établissement, par série). |
| 8 | `bac_etape_08_proclamation.sql` | 31 juillet à 20 août | Résultats **proclamés** ; **140 diplômes** délivrés avec mention : 7 très bien, 30 bien, 60 assez bien, 43 passable (seuils 10, 12, 14, 16). Les élèves et parents voient leur résultat. | **Statistiques des examens**, **Indicateurs et budget** (délai fin des épreuves → proclamation = 28 jours) ; parent ou élève : onglet **Examens** (portail et application mobile, voir plus bas). |
| 9 | `bac_etape_09_apres_resultats.sql` | août à octobre | 12 réclamations (5 acceptées, 5 rejetées dont 2 traitées hors du délai de 15 jours, 2 encore ouvertes) ; **3 rectifications de notes** : un candidat devient admis (diplôme délivré après coup), un autre perd l'admission (**diplôme révoqué**), une rectification est **annulée** ; 54 vérifications publiques de diplômes dont des **codes inconnus répétés** ; budget soldé (821 millions engagés sur 900 prévus) ; situation statistique provisoire de l'année. | **Qualité et réclamations** (réclamations, résultats rectifiés, bouton « Annuler la rectification » sur une réclamation acceptée) ; **Indicateurs et budget** (exécution, bloc « Authentification des diplômes » avec les codes `SIM0000001` et `SIM0000002` suspects) ; **Situation statistique** ; page publique de vérification d'un diplôme. |

## Voir un résultat côté parent ou élève

```sql
-- Un parent d'un élève admis et un parent d'un élève non admis (mot de passe Demo!2026)
(SELECT c.decision, u.email AS parent, el.matricule FROM candidat_examen c JOIN eleve el ON el.id = c.eleve_id
 JOIN lien_parental l ON l.eleve_id = el.id AND l.responsable_legal JOIN utilisateur u ON u.id = l.utilisateur_id
 WHERE c.examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') AND u.email LIKE '%@demo.gn' AND c.decision = 'ADMIS' ORDER BY c.id LIMIT 3)
UNION ALL
(SELECT c.decision, u.email, el.matricule FROM candidat_examen c JOIN eleve el ON el.id = c.eleve_id
 JOIN lien_parental l ON l.eleve_id = el.id AND l.responsable_legal JOIN utilisateur u ON u.id = l.utilisateur_id
 WHERE c.examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026') AND u.email LIKE '%@demo.gn' AND c.decision = 'NON_ADMIS' ORDER BY c.id LIMIT 3);
```
Codes à saisir dans la page publique « Vérification de diplôme » (le diplôme révoqué donne « Révoqué », un code inventé donne « Inconnu ») :

```sql
SELECT d.code, CASE WHEN d.revoque THEN 'REVOQUE' ELSE 'AUTHENTIQUE' END AS attendu
FROM diplome_examen d JOIN candidat_examen c ON c.id = d.candidat_id
WHERE c.examen_id = (SELECT id FROM examen_national WHERE libelle = 'Baccalauréat simulé 2026')
ORDER BY d.revoque DESC, d.id LIMIT 4;
```

## Simplifications à connaître

- Tous les candidats passent **les 7 mêmes épreuves** (la plateforme n'associe pas encore d'épreuves à une série) ; les séries (sciences mathématiques, sciences sociales) servent aux statistiques.
- La classe d'examen est la **12e année** (niveau GN-12) des élèves du jeu de démonstration, et les 14 candidats libres sont des élèves de 11e année de ces lycées : ce sont des données de test.
- Les deux réclamations « ouvertes » de l'étape 9 apparaissent **en retard** : leur échéance (août 2026) est dépassée à la date du jeu de données (31 octobre 2026).
- Les barèmes et seuils sont ceux de la plateforme : admis dès 10/20 ; mentions à 12, 14 et 16.
- Les horaires sont en heure de Conakry (GMT) ; la fuite de sujet, les fraudes, les retards et les ruptures sont des scénarios inventés pour illustrer les écrans.
