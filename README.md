# Plateforme scolaire nationale

Plateforme de gestion scolaire : établissements, classes, inscriptions, présences, notes et bulletins,
emplois du temps, devoirs, annonces, messagerie parents ↔ personnel, frais de scolarité et paiements,
statistiques (dont taux de réussite par établissement, région et pays).

|Partie|Dossier|Technologies|
|-|-|-|
|Serveur (API)|`backend/`|Java 21, Spring Boot 4, Maven, PostgreSQL 17, Flyway|
|Site web|`frontend/`|React 19, TypeScript, Vite, Mantine 9, TanStack Query, React Router 7|
|Application mobile|`mobile/`|Flutter (Dart ≥ 3.4), Provider, Material 3|
|Base de données de développement|`docker-compose.yml`|PostgreSQL 17 dans Docker|
|Mise en production|`deploiement/` + `DEPLOIEMENT.md`|Docker, nginx, scripts de sauvegarde|

Rôles : administrateur technique, ministère, DGECS, direction régionale (IRE), direction préfectorale ou communale (DPE/DCE), chef de centre d'examen,
direction d'établissement, personnel administratif, enseignant, élève, parent (voir `ROLES.md`).
Les parents et élèves utilisent surtout l'application mobile ; le reste se fait sur le site web.

\---

## 1\. Ce qu'il faut installer (nouvelle machine)

|Outil|Version|Sert à|Vérifier|
|-|-|-|-|
|**Git**|récente|récupérer le code|`git --version`|
|**JDK** (Temurin/OpenJDK)|**21**|compiler et lancer le serveur|`java -version`|
|**Maven**|3.9+|construire le serveur|`mvn -version` (doit afficher Java 21)|
|**Docker Desktop** (ou Docker Engine + Compose)|récente|base PostgreSQL de développement **et** tests d'intégration|`docker --version` puis `docker compose version`|
|**Node.js**|**22 LTS** (≥ 20.19)|site web|`node -v` et `npm -v`|
|**Flutter SDK**|stable récent (Dart ≥ 3.4)|application mobile|`flutter --version` puis `flutter doctor`|
|**Android Studio**|récent|SDK Android, émulateur|`flutter doctor` ne doit plus signaler d'erreur Android|
|Un éditeur|—|IntelliJ IDEA (serveur), VS Code (web/mobile)|—|

Seulement pour iOS : un Mac avec Xcode. Sous Windows et Linux, on ne peut cibler qu'Android (et le web).

### Windows

1. Installer **Git**, **Temurin JDK 21**, **Node.js 22 LTS**, **Docker Desktop** (activer WSL 2 quand il le propose)
depuis leurs sites officiels.
2. **Maven** : télécharger l'archive binaire, la décompresser (ex. `C:\\Tools\\maven`), puis ajouter `C:\\Tools\\maven\\bin`
au `PATH` de Windows.
3. Définir la variable d'environnement `JAVA\_HOME` (ex. `C:\\Program Files\\Eclipse Adoptium\\jdk-21...`).
4. **Flutter** : décompresser le SDK (ex. `C:\\src\\flutter`), ajouter `C:\\src\\flutter\\bin` au `PATH`.
Installer Android Studio, ouvrir *SDK Manager* et installer « Android SDK Command-line Tools », puis
`flutter doctor --android-licenses` (répondre `y`).
5. **Fermer puis rouvrir** le terminal pour que le `PATH` soit pris en compte.

### Linux (Debian/Ubuntu) / macOS

* Linux : `sudo apt install git openjdk-21-jdk maven` ; Node via [nvm](https://github.com/nvm-sh/nvm)
(`nvm install 22`) ; Docker Engine selon la documentation officielle ; Flutter par archive ou `snap install flutter --classic`.
* macOS : `brew install git openjdk@21 maven node@22 \&\& brew install --cask docker flutter android-studio`.

### Vérification rapide

```bash
git --version \&\& java -version \&\& mvn -version \&\& node -v \&\& docker --version \&\& flutter --version
flutter doctor
```

Toutes ces commandes doivent répondre sans erreur avant de continuer.

\---

## 2\. Récupérer le projet

```bash
git clone <adresse-du-dépôt> plateforme-scolaire
cd plateforme-scolaire
```

(Sans dépôt : copier le dossier du projet tel quel sur la nouvelle machine, **sans** `node\_modules/`, `target/`,
`build/` et `.dart\_tool/`, qui se régénèrent.)

\---

## 3\. Lancer en développement

Il faut **trois terminaux** : base de données, serveur, site web. Toujours dans cet ordre.

### 3.1 Base de données

```bash
docker compose up -d db
docker compose ps          # l'état doit devenir « healthy »
```

PostgreSQL écoute sur `localhost:5433` (base `scolaire`, utilisateur `scolaire`, mot de passe `scolaire`).
Les données sont conservées dans le volume Docker `scolaire-data`.

Repartir de zéro (efface toutes les données) :

```bash
docker compose down -v \&\& docker compose up -d db
```

### 3.2 Serveur (API) — http://localhost:8080

```bash
cd backend
mvn spring-boot:run
```

* Le profil `dev` est le profil par défaut. Au démarrage, **Flyway crée toutes les tables** (migrations
`backend/src/main/resources/db/migration`, V1 à V30) et des **données de démonstration** sont insérées.
* Attendre la ligne `Started ScolaireApplication`.
* Santé : [http://localhost:8080/api/sante](http://localhost:8080/api/sante) doit répondre `200`.
* Documentation de l'API (dev uniquement) : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
* Les e-mails ne partent pas sans SMTP : en dev, leur contenu (avec les liens) s'écrit dans la console du serveur.

### 3.3 Site web — http://localhost:5173

```bash
cd frontend
npm install        # une seule fois (et après chaque changement de package.json)
npm run dev
```

Le site appelle `/api`, relayé automatiquement vers `http://localhost:8080` (voir `frontend/vite.config.ts`).

### 3.4 Application mobile

**Première fois sur une machine** : les dossiers `android/` et `ios/` ne sont pas dans le projet, Flutter les génère.
Depuis la racine du projet :

```bash
flutter create --org com.plateforme --project-name gestion\_scolaire --platforms android,ios mobile\_tmp
cp -r mobile\_tmp/android mobile\_tmp/ios mobile/        # Windows PowerShell : Copy-Item -Recurse mobile\_tmp\\android,mobile\_tmp\\ios mobile\\
rm -rf mobile\_tmp                                      # Windows PowerShell : Remove-Item -Recurse -Force mobile\_tmp
cd mobile
flutter pub get
```

Réglages Android (développement uniquement) :

* `mobile/android/app/src/main/AndroidManifest.xml`, sur la balise `<application ...>` : ajouter
`android:usesCleartextTraffic="true"` (autorise le HTTP vers le poste de développement ; **à retirer en production**).
* `mobile/android/app/build.gradle(.kts)` : si Gradle se plaint de `flutter\_secure\_storage`, mettre `minSdk = 23`.

Lancer (le serveur doit tourner) :

```bash
cd mobile
flutter devices                                                   # liste émulateurs/téléphones
flutter run                                                       # émulateur Android (10.0.2.2 = ton PC)
flutter run --dart-define=API\_URL=http://192.168.1.20:8080        # téléphone réel : IP de ton PC sur le Wi‑Fi
flutter run --dart-define=API\_URL=http://localhost:8080           # simulateur iOS
```

Téléphone réel : le téléphone et le PC doivent être sur le même réseau, et le pare-feu du PC doit autoriser le port 8080.

\---

## 4\. Comptes de démonstration (profil dev)

Mot de passe de tous les comptes : **`Demo!2026`** (modifiable avec la variable `DEV\_PASSWORD` avant le premier démarrage).

|Compte|Rôle|
|-|-|
|`ministere@example.org`|Ministère (vue nationale, référentiels : années, niveaux, matières)|
|`dgecs@example.org`|DGECS (direction nationale des examens et du contrôle scolaire)|
|`direction1@example.org`|Direction de l'école démo 1|
|`eleve1@example.org`|Élève (utilisable dans l'application mobile)|

Les autres comptes se créent depuis le site, connecté en direction :

* **Enseignant** : menu des enseignants de l'établissement ; il reçoit un mot de passe initial et doit le changer à
la première connexion. Ensuite, l'affecter à une classe et à une matière.
* **Parent** : menu *Élèves → Parents d'un élève*. Même principe de mot de passe initial.

Ordre logique pour une école neuve : année scolaire et périodes → niveaux et matières (ministère) → classes →
inscriptions (acceptées dans une classe) → enseignants affectés → frais.

\---

## 5\. Tests et contrôles

|Quoi|Commande (dans le dossier)|Remarque|
|-|-|-|
|Serveur : compilation + tous les tests|`cd backend \&\& mvn clean verify`|**Docker doit être lancé** : le test d'intégration `ParcoursScolaireIntegrationTest` démarre un vrai PostgreSQL. Sans Docker il est ignoré.|
|Serveur : tests rapides seulement|`mvn test -Dtest='!\*IntegrationTest'`|sans Docker|
|Site web : compilation + types|`cd frontend \&\& npm run build`|échoue sur toute erreur TypeScript|
|Site web : analyse|`npm run lint`||
|Mobile : analyse|`cd mobile \&\& flutter analyze`|doit afficher « No issues found »|
|Mobile : tests|`flutter test`||

Avant toute livraison : les quatre premières lignes doivent passer.

\---

## 6\. Réglages (variables d'environnement)

En développement, **rien à régler**. Les variables ci-dessous servent surtout en production
(liste complète et contexte dans `DEPLOIEMENT.md`).

|Variable|Rôle|Défaut dev|
|-|-|-|
|`SPRING\_PROFILES\_ACTIVE`|`dev` ou `prod`|`dev`|
|`DB\_URL`, `DB\_USER`, `DB\_PASSWORD`|base PostgreSQL|`localhost:5433` / `scolaire` / `scolaire`|
|`JWT\_SECRET`|secret des jetons (32 caractères min.), **obligatoire en prod**|valeur de dev|
|`CORS\_ORIGINS`|adresses autorisées du site web|`localhost:5173`, `localhost:3000`|
|`FRONT\_URL`|adresse du site (liens dans les e-mails)|`http://localhost:5173`|
|`SPRING\_MAIL\_HOST`, `\_PORT`, `\_USERNAME`, `\_PASSWORD`, `MAIL\_DE`|envoi d'e-mails|vide : pas d'envoi|
|`ANNONCES\_EMAIL`|e-mail aux parents à chaque annonce|`true`|
|`SMS\_ANNONCES`|SMS aux parents à chaque annonce (nécessite une passerelle SMS)|`false`|
|`DOUBLE\_AUTHENTIFICATION`|Code à 6 chiffres envoyé par e-mail à chaque connexion du ministère et des directions régionales (SMTP requis)|`false`|
|`TAILLE\_ENVOI\_MAX` / `PIECE\_JOINTE\_MAX`|Taille maximale d'une requête d'envoi de pièce jointe / d'un fichier|`6MB` / `5MB`|
|`SMS\_ABSENCES`|SMS aux parents quand leur enfant est noté absent (non justifié) à l'appel|`false`|
|`SMS\_ALERTES`|SMS d'alerte d'inspection urgente au ministère et aux directions régionales|`true`|
|`SMS\_JOURNALISER`|écrit les SMS dans les journaux quand aucune passerelle n'est branchée (développement)|`false`|
|`ALERTES\_ACTIF` / `ALERTES\_CRON`|alertes d'inspection urgente, et heure d'exécution|`true` / `0 0 7 \* \* \*`|
|`RAPPORTS\_ENTETE` / `RAPPORTS\_SOUS\_ENTETE`|en-tête des rapports PDF officiels|`Ministère de l'Éducation` / `République de Guinée`|
|`ELEVES\_PAR\_ENSEIGNANT`|norme servant à estimer les besoins en enseignants|`40`|
|`TAILLE\_REQUETE\_MAX`|taille maximale d'une requête|`1MB`|
|`REUSSITE\_CACHE\_MINUTES`|cache du taux de réussite national|`10`|
|`PAIEMENT\_FOURNISSEUR`, `PAIEMENT\_WEBHOOK\_SECRET`|paiement en ligne (`simule` en dev, vide = désactivé)|`simule`|
|`API\_URL` (mobile, `--dart-define`)|adresse du serveur|`http://10.0.2.2:8080`|
|`VITE\_API\_URL` (site web, à la compilation)|adresse du serveur si différente du site|vide (même origine)|

Sous PowerShell, définir une variable pour une session : `$env:DEV\_PASSWORD = "MonMotDePasse1!"`.

\---

## 7\. Structure du projet

```
backend/src/main/java/com/plateforme/scolaire/
  config/      sécurité (JWT, CORS, limites)      domain/      entités JPA
  repository/  accès base de données              service/     logique métier
  security/    périmètres d'accès, filtres        web/         contrôleurs REST + dto/
  dev/         données de démonstration (profil dev uniquement)
backend/src/main/resources/   application\*.yml et db/migration (Flyway)
backend/src/test/             tests unitaires et integration/ (parcours complet)
frontend/src/                 api/ (client, types), pages/, components/, layout/, hooks/
mobile/lib/                   api/, ecrans/, widgets/, auth/, utils/   et mobile/test/
deploiement/                  Dockerfiles, nginx, compose de production, sauvegardes
```

Règles à connaître :

* **Le schéma de la base ne se modifie que par une nouvelle migration** `V17\_\_...sql`, jamais en éditant une ancienne
(Hibernate est en mode `validate` : il refuse de démarrer si une entité ne correspond pas au schéma).
* Chaque route est protégée par un contrôle de périmètre (`@PreAuthorize`) : un utilisateur ne voit que son
établissement, sa classe, ses enfants.
* Les droits sur les examens sont centralisés dans `security/DroitsExamen` (constantes `@PreAuthorize`) ; l'accès d'un chef de centre à ses centres
passe par `@accesCentre.peut(#centreId)`. Détail dans `ROLES.md`.
* Le **programme officiel** (matières, heures par semaine, coefficients par niveau et série, année par année) est une donnée modifiable
(Référentiels > Programme officiel), pas du code : la grille du COC 2023 (`backend/src/main/resources/referentiel/coc-2023.csv`) n'est qu'un point de départ
importable, car les horaires seront stabilisés par le ministère. Les séries du lycée (sciences mathématiques, sociales, expérimentales) sont portées
par les classes et recopiées sur les candidats aux examens.
* **Tableau de bord DGECS** (page « DGECS : pilotage des examens », la DGECS gère, le ministère consulte). Onglets et migrations associées :

  * *Statistiques des examens* : salles, centres, niveaux d'examen, types d'établissement, séries du lycée.
  * *Candidats et résultats* : filles/garçons, libres/scolaires, participation, réussite sur présents et sur inscrits, mentions, zones, évolution N / N-1.
  * *Qualité et réclamations* (V55) : dossiers incomplets, notes manquantes, listes validées ; réclamations déposées après proclamation et traitées
dans un délai de 15 jours (constante `ReclamationsExamenService.DELAI\_JOURS`, à confirmer avec la DGECS).
  * *Logistique des centres* (V56) : personnel requis/pourvu (mêmes règles que la page du centre) et fournitures livrées (sujets, copies, enveloppes, fournitures) ; centre « prêt » ou « en retard ».
  * *Indicateurs et budget* (V57) : indicateurs de performance du directeur général et budget par rubrique en francs guinéens (prévu, engagé, payé, coût par candidat).
  * *Copies et fiabilité* : correction, fraudes, doublons, notes manquantes, moyennes par discipline.
  * *Logistique des centres* (suite, V59) : état des centres (salles de réserve, installations, accessibilité, distances, risques d'inondation, d'enclavement et d'insécurité) et candidats en situation de handicap (type et aménagement déclarés depuis la liste des candidats de l'établissement, donnée sensible).
  * *Personnel mobilisé* (V61, sous « Logistique des centres ») : requis, disponible et manque par fonction. Chefs de centre, secrétaires et surveillants (2 par salle) sont calculés ; sécurité, santé, correcteurs, jurys, superviseurs, inspecteurs et agents informatiques ont un besoin déclaré par la DGECS ; jurys, superviseurs, inspecteurs et informatique se saisissent nominativement.
  * *Transport des sujets et du matériel* (V62, sous « Logistique des centres ») : convois par centre (véhicule, chauffeur, itinéraire, escorte, horaires prévus et réels, carburant, statut, incident), centres desservis et sans convoi, part des livraisons à temps. Saisie réservée à la DGECS.
  * *Sujets à imprimer* : par épreuve, sujets (candidats + 5 % de marge), feuilles (2 par candidat), enveloppes (une par salle) et écart avec les exemplaires conditionnés ; par série. Seuls des effectifs sont affichés, jamais le contenu des sujets. Marge et nombre de feuilles sont des hypothèses à valider avec la DGECS.
  * *Besoins en personnel par épreuve* (V63, sous « Logistique des centres ») : la DGECS déclare, épreuve par épreuve, les besoins en sécurité, santé, correcteurs, jurys, superviseurs, inspecteurs et informatique ; le plus grand besoin (pic) est comparé au personnel disponible pour donner le manque.
  * *Authentification des diplômes* (V64 et V65, onglet des indicateurs) : chaque contrôle public est journalisé (code saisi tronqué et résultat, rien sur la personne) ; synthèse des vérifications, part de codes inconnus et codes inconnus saisis au moins 3 fois en 30 jours (seuil à valider).
  * *Annulation d'une rectification* (V64, sous « Réclamations ») : rétablit la note, la moyenne et la décision d'origine, recalcule les totaux de l'établissement, rétablit le diplôme révoqué par cette rectification et rouvre la réclamation ; seule la dernière rectification d'un candidat peut être annulée, avec motif obligatoire.
  * *Participation* : présents, absents, copies ramassées et ponctualité par épreuve puis par centre, d'après les rapports de surveillance déjà saisis dans les centres (tolérance de 15 minutes, `ParticipationExamensService.TOLERANCE\_MINUTES`).
  * *Situation statistique* (V58) : chiffres figés à une date, provisoires puis définitifs (une seule situation définitive par année, possible quand tous les résultats sont proclamés).
* Tout ce qui est sensible (changement de note, etc.) est écrit dans le journal d'audit.

\---

## 8\. Problèmes fréquents

|Symptôme|Cause / remède|
|-|-|
|`Port 5433 already in use`|un autre PostgreSQL tourne : l'arrêter, ou changer le port dans `docker-compose.yml` **et** `DB\_URL`.|
|Le serveur ne démarre pas : `Connection refused`|la base n'est pas prête : `docker compose ps` doit afficher `healthy`.|
|`Schema-validation: missing table/column`|base issue d'une ancienne version : `docker compose down -v \&\& docker compose up -d db`, puis relancer le serveur.|
|`mvn` utilise la mauvaise version de Java|vérifier `JAVA\_HOME` et `mvn -version` (Java 21 obligatoire).|
|Tests d'intégration « ignorés »|Docker n'est pas lancé : ouvrir Docker Desktop.|
|Le site affiche une erreur réseau|le serveur n'est pas lancé, ou n'écoute pas sur 8080.|
|Mobile : « serveur injoignable »|émulateur : `10.0.2.2`, téléphone réel : `--dart-define=API\_URL=http://IP-du-PC:8080`, et pare-feu ouvert.|
|Mobile : erreur Gradle `flutter\_secure\_storage`|mettre `minSdk = 23`.|
|Compte verrouillé (5 échecs)|attendre, ou en dev : `update utilisateur set verrouille\_jusqu\_a = null where email = '...'`.|
|`flutter doctor` signale des licences|`flutter doctor --android-licenses`.|

\---

## 9\. Mise en production

Voir **`DEPLOIEMENT.md`** (variables, protections, hébergement) et **`deploiement/CHECKLIST\_OUVERTURE.md`**
(étapes avant ouverture). Résumé :

```bash
cp deploiement/.env.example deploiement/.env      # puis remplir les secrets (openssl rand -base64 48)
docker compose -f deploiement/docker-compose.prod.yml --env-file deploiement/.env up -d --build
```

Prévoir un HTTPS devant le service `web` (port 8081), les sauvegardes quotidiennes (`deploiement/sauvegarde.sh`, `verifier-sauvegarde.sh` et `supervision.sh` : voir `deploiement/SAUVEGARDES\_ET\_SUPERVISION.md`) et un
essai de restauration (`deploiement/restauration.sh`) avant l'ouverture. Pour l'application mobile en production :
`flutter build apk --dart-define=API\_URL=https://ton-domaine` et retirer `usesCleartextTraffic`.

\---

## 10\. Pas encore fait

* Paiement réel **Orange Money** : l'adaptateur attend le contrat et les identifiants (en attendant :
`PAIEMENT\_FOURNISSEUR` vide en production, `simule` seulement en développement).
* Jetons stockés dans le navigateur et non dans des cookies `HttpOnly` (limite connue, voir `DEPLOIEMENT.md`).
* **Diplômes** : le PDF reprend les champs du diplôme officiel (session, profil, PV, centre, école d'origine, jury, état civil, filiation,
numéro d'enregistrement, QR de vérification), mais sans signature, sceau, armoiries ni photo : la DGECS les appose sur le papier imprimé.
La photo d'identité est téléversée depuis la fiche de l'élève (JPEG ou PNG, 1 Mo au plus).
* **Barème des examens** : fixé à la création de l'examen (10 pour le primaire, 20 sinon) ; la DGECS peut le modifier (calendrier de l'examen)
tant qu'aucune note n'est saisie. Mentions : 50 % du barème = passable, 60 % assez bien, 70 % bien, 80 % très bien.
* **Programme officiel** : la grille du COC 2023 ne couvre pas la 12e et la Terminale (à saisir dans Référentiels > Programme officiel) ;
l'attribution des colonnes littéraire/scientifique de la 11e aux trois séries est à confirmer.
* **Réclamations** : accepter une réclamation sur une note peut rectifier la note après proclamation (V60) : moyenne et décision recalculées, résultats de l'établissement ajustés, diplôme révoqué si le candidat n'est plus admis ; un candidat devenu admis reçoit son diplôme par « Délivrer les diplômes ». L'ancienne et la nouvelle valeur sont conservées. Les résultats par série ne sont ajustés que s'ils ont été saisis.
* **Logistique et budget** : le transport (véhicules, carburant, itinéraires) et la sécurité des convois ne sont pas suivis ; le budget se saisit par rubrique,
sans détail des dépenses (fournisseur, date). Les rubriques et le délai de 15 jours pour les réclamations sont des propositions à valider avec la DGECS.

