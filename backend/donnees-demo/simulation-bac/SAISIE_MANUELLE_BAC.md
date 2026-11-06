# Saisie manuelle de la simulation BAC : écrans à utiliser, pas à pas

Ce guide suit les 9 étapes des scripts SQL (`bac_etape_01` … `bac_etape_09`).
Pour chaque étape : le compte à utiliser, l'écran, l'action.

> Les libellés exacts des boutons peuvent différer légèrement de ceux écrits ici. Rien n'a été exécuté ni compilé pour rédiger ce guide ; il est fondé sur la lecture du code des écrans.
> Saisir 331 candidats et 2 170 notes à la main n'est pas raisonnable : saisissez un **échantillon** (ex. 1 établissement, 10 candidats, 1 centre) pour valider le parcours, et utilisez les scripts SQL pour le volume.

> Dans l'accueil de l'établissement, choisissez l'année **2025-2026** (sélecteur en haut) pour voir l'examen de simulation.

## Comptes et rôles

| Rôle | Sert à |
|---|---|
| DGECS | Planification, validation, centres, affectation, sujets, convois, besoins, budget, jury, diplômes |
| MINISTERE | Consultation et **proclamation** des résultats uniquement |
| DIRECTION_REGIONALE (IRE) / DIRECTION_LOCALE (DPE) | Vérification des listes, actions de gestion dans leur périmètre |
| Direction d'établissement | Inscription des candidats, pièces, aménagements, transmission de la liste |
| CHEF_CENTRE | Tout ce qui se passe dans son centre (`/mon-centre`) |

## Étape 1 : Préparation de l'examen (compte DGECS)

1. Espace ministère → onglet **Examens** → vue de planification → **Planifier un examen** : type BAC, année scolaire, dates, barème (20), seuil d'admission.
   Choisissez le **cycle Lycée** : c'est lui qui active les séries. Le niveau choisi doit appartenir au cycle.
2. Même écran, ajouter les **7 épreuves** (matière, coefficient, horaire, date). Pour un examen du lycée, choisissez la **série** de chaque épreuve (vide = commune à toutes les séries) : chaque série a ainsi son propre emploi du temps, et un candidat ne compose que les épreuves communes et celles de sa série.
3. Onglet **Logistique** : renseigner les **besoins** globaux, puis les besoins **par épreuve** (écran « Besoins par épreuve »).
4. Onglet **Budget** : bouton **Modifier** sur chaque rubrique pour saisir le budget prévu.

## Étape 2 : Candidatures (compte direction d'établissement)

1. Accueil établissement → **Examens** → choisir l'examen → **Candidats**.
2. Inscrire chaque candidat (élève de l'établissement ; candidat libre pour les non scolarisés). Un candidat libre doit être du cycle de l'examen et, au lycée, **déclarer sa série**.
3. Ouvrir la fiche du candidat : ajouter les **pièces** (acte de naissance, photo…) et les **aménagements** (tiers-temps, etc.).
4. **Transmettre** la liste de l'établissement.
5. Compte IRE/DPE : écran de **vérification des listes** → valider ou renvoyer (c'est ainsi qu'une liste « renvoyée » apparaît).
6. Compte DGECS : **valider** les listes.

## Étape 3 : Centres d'examen (compte DGECS, puis chef de centre)

1. DGECS → **Examens** → **Calendrier et centres** : désigner les centres, saisir leurs caractéristiques (capacité, salles).
2. Lancer l'**affectation automatique** des candidats aux centres ; vérifier le résultat (PV, salle, table).
3. Affecter le personnel (surveillants, chef de centre) et créer les comptes `CHEF_CENTRE` pour chaque centre.
4. Chef de centre (`/mon-centre`) : compléter la **préparation du centre** et le **personnel** local.

## Étape 4 : Sujets et logistique

1. DGECS → **Banque de sujets** (modale de l'examen) : saisir les sujets, **Valider**, **Tirer au sort** (tirage A/B) ; en cas de fuite : **Fuite : remplacer**.
2. Onglet **Logistique** → **Transport et sujets** : enregistrer les **convois** (départ, arrivée, retards) et consulter les **sujets à imprimer**.
3. Logistique → bouton **Fournitures** d'un centre : article, quantité prévue, statut (Reçue…), quantité reçue, observation → **Enregistrer** (c'est l'écran des livraisons).
4. Budget : **Modifier** une rubrique pour saisir le montant engagé.

## Étape 5 : Déroulement des épreuves (chef de centre, `/mon-centre`)

1. **Déroulement** : suivre le circuit du pli avec les boutons *Marquer conditionné*, *Marquer expédié à l'IRE*, *Marquer reçu par la DPE/DCE*, *Marquer reçu par le président*, puis **Ouvrir le pli** (deux témoins).
2. Saisir les **présences / absences** par épreuve.
3. Saisir le **rapport de surveillance** (copies ramassées) et le **clôturer**.
4. Déclarer les **fraudes** et **incidents**.

## Étape 6 : Correction et notes

1. DGECS → **Correction** : générer les **sous-commissions**, affecter les correcteurs.
2. **Résultats → Par établissement → Notes** (modale des notes des candidats) : pour chaque candidat, choisir le statut (*À saisir*, *Présent*, *Absent*, *Éliminé (fraude)*) et saisir la note, puis **Enregistrer**.
3. **Clôturer** la correction. La moyenne ne se calcule que lorsque **toutes** les épreuves ont une note.

## Étape 7 : Délibération (DGECS)

Modale **Délibération** : **initialiser le jury** (membres, présence), **délibérer** (décisions admis/non admis, mentions), puis **Établir les résultats de l'établissement** si l'écran le propose.
*Rouvrir* permet de revenir en arrière tant que rien n'est proclamé.

## Étape 8 : Proclamation et diplômes

1. **Compte MINISTERE** : même modale de délibération → **Proclamer**.
2. Compte DGECS : modale **Diplômes** → **Délivrer les diplômes des admis**.

## Étape 9 : Après les résultats

1. **Réclamations** : les candidats ou établissements les déposent ; DGECS les traite (acceptée / rejetée) dans l'onglet **Qualité → Réclamations**.
2. **Rectification de note** : depuis une réclamation acceptée ; l'**annulation** d'une rectification se fait dans la même page (motif obligatoire).
3. **Diplômes** : **Révoquer** (avec **Motif**) en cas de fraude.
4. **Vérification publique** : page publique de vérification des diplômes. Les journaux de vérification **ne se saisissent pas à la main** : ils sont créés uniquement quand quelqu'un utilise cette page. Pour simuler un diplôme suspect, saisissez plusieurs fois le même code.
5. Onglets **Budget** (solde des rubriques) et **Situation** (situation statistique provisoire de l'année).

## Ce qui n'a pas d'écran de saisie

- Les journaux de vérification de diplômes (voir étape 9).
- Les volumes (331 candidats, 2 170 notes) : les scripts SQL restent la méthode réaliste.
- Certaines données de la simulation (heures de retard précises, comptes `chef.bac1..7@demo.gn`) sont posées par SQL ; à la main, vous les créez avec vos propres valeurs.

## Mixte recommandé

Faire à la main les étapes 1 et 3 (paramétrage, centres) et un petit échantillon des étapes 2, 5 et 6, puis charger le reste avec `charger_etapes` pour obtenir un jeu complet.
