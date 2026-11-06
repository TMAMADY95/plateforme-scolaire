# Données de démonstration — organisation des examens, de bout en bout

Simulation complète : CEE, BEPC, BAC (chaque série), examen communal de 9e année ; enseignement **général**, **franco-arabe**
et **technique** ; établissements classiques, communautaires, confessionnels, franco-arabes et techniques, publics et privés.
**Mot de passe de tous les comptes : `Demo!2026`.** Toutes les données sont fictives.

## Installation

Le backend doit avoir été démarré une fois (Flyway crée le schéma). Arrêtez-le pendant l'exécution, puis :

```bash
./scripts/demo/installer.sh --vider              # tout : vide les données, puis scripts 1 à 6
./scripts/demo/installer.sh --vider --jusqu-a 1  # établissements et élèves seulement : vous organisez tout vous-même
./scripts/demo/installer.sh --jusqu-a 3          # sans vider (base déjà vide) ; jusqu'aux inscriptions
```
Avec Docker (PowerShell), depuis la racine du projet :
```powershell
foreach ($f in Get-ChildItem .\scripts\demo\0*.sql) {
  docker cp $f.FullName scolaire-db:/tmp/$($f.Name)
  docker exec -e PGCLIENTENCODING=UTF8 scolaire-db psql -U scolaire -d scolaire -v ON_ERROR_STOP=1 -1 -f /tmp/$($f.Name)
}
```
(le script `00` supprime toutes les données : retirez-le de la boucle si ce n'est pas voulu). Les scripts s'exécutent dans l'ordre 01 → 06 ;
chacun est une transaction (`-1`) : en cas d'erreur, rien n'est enregistré par ce script et on peut le relancer.
Le message `WARNING: there is already a transaction in progress` est sans gravité.

| Script | Étape d'organisation couverte |
|---|---|
| `00` | Vide les données (garde structure, Flyway et référentiels des migrations) |
| `01` | Années 2025-2026 et 2026-2027, niveaux, matières, zones, **20 établissements des 5 types**, 63 comptes, classes, ~2 000 élèves |
| `02` | **Planification** : 18 examens, calendrier par série, banque de sujets, besoins et personnel national, budget |
| `03` | **Inscriptions** : candidats réguliers et libres, listes (tous les états), dossiers et pièces, handicaps et aménagements |
| `04` | **Préparation** : centres, chefs de centre, état des locaux, travaux, salles, PV, personnel, livraisons, convois, plis |
| `05` | **Déroulement et correction** : présents/absents/éliminés, ouverture des plis, incidents, commissions, notes, moyennes |
| `06` | **Résultats** : réclamations, rectifications, résultats par établissement et série, jury, délibération, proclamation, diplômes |

## Les 18 examens et leur état (tous les stades du circuit)

Aujourd'hui = novembre 2026 : l'année 2025-2026 est terminée (examens passés), 2026-2027 est en cours (examens à venir).

| Examen | Stade | Ce que vous y trouvez |
|---|---|---|
| **BAC général 2026** | Proclamé | Tout le circuit : fuite de sujet (maths SM), incident de convoi, fraude, diplômes dont 2 révoqués, 9 réclamations (rectifications acceptées, une annulée, rejets, réclamations ouvertes en retard), vérifications de diplômes |
| BAC franco-arabe 2026 | Délibéré | Jury réuni (quorum des deux tiers), résultats par série ; **proclamation à faire** |
| BAC technique 2026 | Correction | Commissions en cours, notes manquantes pour ~45 % des candidats, aucune décision définitive |
| BEPC général 2026 | Proclamé | Diplômes, réclamations, rectifications, accident de convoi |
| BEPC franco-arabe 2026 | Déroulement | Épreuves passées, plis du dernier jour au rapport non clos, **incidents ouverts**, aucune note |
| BEPC technique 2026 | Délibéré | Résultats consolidés, proclamation à faire |
| CEE général 2026 | Proclamé | Barème /10, diplômes, réclamations ; carton perdu lors du convoi |
| CEE franco-arabe 2026 | Correction | Notes partielles |
| Examen communal 9e 2026 | Proclamé | Organisation au niveau de la commune, tous types d'établissement |
| **BAC général 2027** | Préparation avancée | Listes validées, centres, salles et PV attribués, personnel, livraisons expédiées, convois en route, plis conditionnés/expédiés |
| BAC franco-arabe 2027 | Inscriptions | Listes à tous les états : brouillon, transmise, validée par la région, rejetée (motif), validée |
| BAC technique 2027 | Planification | Épreuves, sujets en élaboration, besoins ; aucun candidat |
| BEPC général 2027 | Préparation initiale | Centres désignés, **répartition en salles à faire**, préparation non commencée |
| BEPC franco-arabe 2027 | Inscriptions | Candidats libres, dossiers incomplets, listes à divers états |
| BEPC technique 2027 | Planification | Épreuves et besoins seulement |
| CEE général 2027 | Inscriptions | Listes à tous les états |
| CEE franco-arabe 2027 | Planification | Épreuves seulement |
| Examen communal 9e 2027 | Inscriptions | Listes à tous les états sur 10 établissements |

## Cas particuliers volontairement présents

- **Établissements** : en attente d'autorisation, autorisation retirée (aucun candidat) ; 5 types ; public et privé.
- **BAC** : séries SM, SE et SS avec calendriers et coefficients différents ; épreuves d'arabe et d'éducation islamique en franco-arabe ; série technique sans série.
- **Candidats** : libres (BEPC et BAC) avec pièces propres ; dossiers incomplets ; handicaps (moteur, visuel, auditif, intellectuel, autre) avec aménagement ; absents ; éliminés pour fraude.
- **Logistique** : livraison incomplète, convoi en retard (Kankan), accident, perte ; centres à risque (inondation, enclavement, insécurité) et locaux en mauvais état.
- **Déroulement** : plis ouverts avec deux témoins, incidents de tous types (fraude, retard, malaise, perturbation, fuite de sujet, autre), ouverts et traités.
- **Résultats** : réclamations dans tous les états (acceptée, rejetée, ouverte en retard), rectifications dont une annulée, diplômes authentiques et révoqués, vérifications publiques.

## Comptes

| Rôle | E-mail |
|---|---|
| Ministère | `ministere@example.org` |
| DGECS | `dgecs@example.org` |
| Directions régionales | `dr.conakry@…`, `dr.kindia@…`, `dr.kankan@…` |
| Directions communales | `dl.kaloum@…`, `dl.dixinn@…`, `dl.kindia@…`, `dl.kankan@…` |
| Directions d'établissement (20) | `direction.<région>.<commune>.<type>@…` : `direction.cky.kal.ly@…` (Lycée Kaloum), `direction.cky.dix.fa@…` (Al-Nour), `direction.kan.kan.fa@…` (Institut franco-arabe de Kankan)… |
| Chefs de centre (34, un par centre) | `chef.bac.g.2027.kal@…`, `chef.bepc.fa.2026.kan@…`… (`chef.<examen>.<3 premières lettres de la commune>@…`) |

## Ce qui n'est pas simulé

Présences et absences scolaires, emplois du temps, évaluations de classe, enseignants, frais et paiements, discipline, transferts :
ces modules ne font pas partie de l'organisation des examens. Les examens n'ont pas de valeur officielle.

## Hypothèses à valider avec le ministère

Barème /10 au CEE, seuil d'admission (moitié du barème), coefficients du BAC par série, organisation (CEE préfecture, BEPC région,
BAC national, examen communal), liste des matières, composition des jurys, structure des centres (un par commune).
