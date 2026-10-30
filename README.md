# Plateforme scolaire nationale

Plateforme de gestion scolaire : établissements, classes, inscriptions, présences, notes et bulletins,
emplois du temps, devoirs, annonces, messagerie parents ↔ personnel, frais de scolarité et paiements,
statistiques (dont taux de réussite par établissement, région et pays).

| Partie | Dossier | Technologies |
|---|---|---|
| Serveur (API) | `backend/` | Java 21, Spring Boot 4, Maven, PostgreSQL 17, Flyway |
| Site web | `frontend/` | React 19, TypeScript, Vite, Mantine 9, TanStack Query, React Router 7 |
| Application mobile | `mobile/` | Flutter (Dart ≥ 3.4), Provider, Material 3 |
| Base de données de développement | `docker-compose.yml` | PostgreSQL 17 dans Docker |
| Mise en production | `deploiement/` + `DEPLOIEMENT.md` | Docker, nginx, scripts de sauvegarde |

Rôles : ministère, direction d'établissement, personnel administratif, enseignant, élève, parent.
Les parents et élèves utilisent surtout l'application mobile ; le reste se fait sur le site web.

---

## 1. Ce qu'il faut installer (nouvelle machine)

| Outil | Version | Sert à | Vérifier |
|---|---|---|---|
| **Git** | récente | récupérer le code | `git --version` |
| **JDK** (Temurin/OpenJDK) | **21** | compiler et lancer le serveur | `java -version` |
| **Maven** | 3.9+ | construire le serveur | `mvn -version` (doit afficher Java 21) |
| **Docker Desktop** (ou Docker Engine + Compose) | récente | base PostgreSQL de développement **et** tests d'intégration | `docker --version` puis `docker compose version` |
| **Node.js** | **22 LTS** (≥ 20.19) | site web | `node -v` et `npm -v` |
| **Flutter SDK** | stable récent (Dart ≥ 3.4) | application mobile | `flutter --version` puis `flutter doctor` |
| **Android Studio** | récent | SDK Android, émulateur | `flutter doctor` ne doit plus signaler d'erreur Android |
| Un éditeur | — | IntelliJ IDEA (serveur), VS Code (web/mobile) | — |

Seulement pour iOS : un Mac avec Xcode. Sous Windows et Linux, on ne peut cibler qu'Android (et le web).

### Windows

1. Installer **Git**, **Temurin JDK 21**, **Node.js 22 LTS**, **Docker Desktop** (activer WSL 2 quand il le propose)
   depuis leurs sites officiels.
2. **Maven** : télécharger l'archive binaire, la décompresser (ex. `C:\Tools\maven`), puis ajouter `C:\Tools\maven\bin`
   au `PATH` de Windows.
3. Définir la variable d'environnement `JAVA_HOME` (ex. `C:\Program Files\Eclipse Adoptium\jdk-21...`).
4. **Flutter** : décompresser le SDK (ex. `C:\src\flutter`), ajouter `C:\src\flutter\bin` au `PATH`.
   Installer Android Studio, ouvrir *SDK Manager* et installer « Android SDK Command-line Tools », puis
   `flutter doctor --android-licenses` (répondre `y`).
5. **Fermer puis rouvrir** le terminal pour que le `PATH` soit pris en compte.

### Linux (Debian/Ubuntu) / macOS

- Linux : `sudo apt install git openjdk-21-jdk maven` ; Node via [nvm](https://github.com/nvm-sh/nvm)
  (`nvm install 22`) ; Docker Engine selon la documentation officielle ; Flutter par archive ou `snap install flutter --classic`.
- macOS : `brew install git openjdk@21 maven node@22 && brew install --cask docker flutter android-studio`.

### Vérification rapide

```bash
git --version && java -version && mvn -version && node -v && docker --version && flutter --version
flutter doctor
```

Toutes ces commandes doivent répondre sans erreur avant de continuer.

---

## 2. Récupérer le projet

```bash
git clone <adresse-du-dépôt> plateforme-scolaire
cd plateforme-scolaire
```

(Sans dépôt : copier le dossier du projet tel quel sur la nouvelle machine, **sans** `node_modules/`, `target/`,
`build/` et `.dart_tool/`, qui se régénèrent.)

---

## 3. Lancer en développement

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
docker compose down -v && docker compose up -d db
```

### 3.2 Serveur (API) — http://localhost:8080

```bash
cd backend
mvn spring-boot:run
```

- Le profil `dev` est le profil par défaut. Au démarrage, **Flyway crée toutes les tables** (migrations
  `backend/src/main/resources/db/migration`, V1 à V15) et des **données de démonstration** sont insérées.
- Attendre la ligne `Started ScolaireApplication`.
- Santé : <http://localhost:8080/api/sante> doit répondre `200`.
- Documentation de l'API (dev uniquement) : <http://localhost:8080/swagger-ui.html>.
- Les e-mails ne partent pas sans SMTP : en dev, leur contenu (avec les liens) s'écrit dans la console du serveur.

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
flutter create --org com.plateforme --project-name gestion_scolaire --platforms android,ios mobile_tmp
cp -r mobile_tmp/android mobile_tmp/ios mobile/        # Windows PowerShell : Copy-Item -Recurse mobile_tmp\android,mobile_tmp\ios mobile\
rm -rf mobile_tmp                                      # Windows PowerShell : Remove-Item -Recurse -Force mobile_tmp
cd mobile
flutter pub get
```

Réglages Android (développement uniquement) :

- `mobile/android/app/src/main/AndroidManifest.xml`, sur la balise `<application ...>` : ajouter
  `android:usesCleartextTraffic="true"` (autorise le HTTP vers le poste de développement ; **à retirer en production**).
- `mobile/android/app/build.gradle(.kts)` : si Gradle se plaint de `flutter_secure_storage`, mettre `minSdk = 23`.

Lancer (le serveur doit tourner) :

```bash
cd mobile
flutter devices                                                   # liste émulateurs/téléphones
flutter run                                                       # émulateur Android (10.0.2.2 = ton PC)
flutter run --dart-define=API_URL=http://192.168.1.20:8080        # téléphone réel : IP de ton PC sur le Wi‑Fi
flutter run --dart-define=API_URL=http://localhost:8080           # simulateur iOS
```

Téléphone réel : le téléphone et le PC doivent être sur le même réseau, et le pare-feu du PC doit autoriser le port 8080.

---

## 4. Comptes de démonstration (profil dev)

Mot de passe de tous les comptes : **`Demo!2026`** (modifiable avec la variable `DEV_PASSWORD` avant le premier démarrage).

| Compte | Rôle |
|---|---|
| `ministere@example.org` | Ministère (vue nationale, référentiels : années, niveaux, matières) |
| `direction1@example.org` | Direction de l'école démo 1 |
| `eleve1@example.org` | Élève (utilisable dans l'application mobile) |

Les autres comptes se créent depuis le site, connecté en direction :

- **Enseignant** : menu des enseignants de l'établissement ; il reçoit un mot de passe initial et doit le changer à
  la première connexion. Ensuite, l'affecter à une classe et à une matière.
- **Parent** : menu *Élèves → Parents d'un élève*. Même principe de mot de passe initial.

Ordre logique pour une école neuve : année scolaire et périodes → niveaux et matières (ministère) → classes →
inscriptions (acceptées dans une classe) → enseignants affectés → frais.

---

## 5. Tests et contrôles

| Quoi | Commande (dans le dossier) | Remarque |
|---|---|---|
| Serveur : compilation + tous les tests | `cd backend && mvn clean verify` | **Docker doit être lancé** : le test d'intégration `ParcoursScolaireIntegrationTest` démarre un vrai PostgreSQL. Sans Docker il est ignoré. |
| Serveur : tests rapides seulement | `mvn test -Dtest='!*IntegrationTest'` | sans Docker |
| Site web : compilation + types | `cd frontend && npm run build` | échoue sur toute erreur TypeScript |
| Site web : analyse | `npm run lint` | |
| Mobile : analyse | `cd mobile && flutter analyze` | doit afficher « No issues found » |
| Mobile : tests | `flutter test` | |

Avant toute livraison : les quatre premières lignes doivent passer.

---

## 6. Réglages (variables d'environnement)

En développement, **rien à régler**. Les variables ci-dessous servent surtout en production
(liste complète et contexte dans `DEPLOIEMENT.md`).

| Variable | Rôle | Défaut dev |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` ou `prod` | `dev` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | base PostgreSQL | `localhost:5433` / `scolaire` / `scolaire` |
| `JWT_SECRET` | secret des jetons (32 caractères min.), **obligatoire en prod** | valeur de dev |
| `CORS_ORIGINS` | adresses autorisées du site web | `localhost:5173`, `localhost:3000` |
| `FRONT_URL` | adresse du site (liens dans les e-mails) | `http://localhost:5173` |
| `SPRING_MAIL_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD`, `MAIL_DE` | envoi d'e-mails | vide : pas d'envoi |
| `ANNONCES_EMAIL` | e-mail aux parents à chaque annonce | `true` |
| `TAILLE_REQUETE_MAX` | taille maximale d'une requête | `1MB` |
| `REUSSITE_CACHE_MINUTES` | cache du taux de réussite national | `10` |
| `PAIEMENT_FOURNISSEUR`, `PAIEMENT_WEBHOOK_SECRET` | paiement en ligne (`simule` en dev, vide = désactivé) | `simule` |
| `API_URL` (mobile, `--dart-define`) | adresse du serveur | `http://10.0.2.2:8080` |
| `VITE_API_URL` (site web, à la compilation) | adresse du serveur si différente du site | vide (même origine) |

Sous PowerShell, définir une variable pour une session : `$env:DEV_PASSWORD = "MonMotDePasse1!"`.

---

## 7. Structure du projet

```
backend/src/main/java/com/plateforme/scolaire/
  config/      sécurité (JWT, CORS, limites)      domain/      entités JPA
  repository/  accès base de données              service/     logique métier
  security/    périmètres d'accès, filtres        web/         contrôleurs REST + dto/
  dev/         données de démonstration (profil dev uniquement)
backend/src/main/resources/   application*.yml et db/migration (Flyway)
backend/src/test/             tests unitaires et integration/ (parcours complet)
frontend/src/                 api/ (client, types), pages/, components/, layout/, hooks/
mobile/lib/                   api/, ecrans/, widgets/, auth/, utils/   et mobile/test/
deploiement/                  Dockerfiles, nginx, compose de production, sauvegardes
```

Règles à connaître :

- **Le schéma de la base ne se modifie que par une nouvelle migration** `V16__...sql`, jamais en éditant une ancienne
  (Hibernate est en mode `validate` : il refuse de démarrer si une entité ne correspond pas au schéma).
- Chaque route est protégée par un contrôle de périmètre (`@PreAuthorize`) : un utilisateur ne voit que son
  établissement, sa classe, ses enfants.
- Tout ce qui est sensible (changement de note, etc.) est écrit dans le journal d'audit.

---

## 8. Problèmes fréquents

| Symptôme | Cause / remède |
|---|---|
| `Port 5433 already in use` | un autre PostgreSQL tourne : l'arrêter, ou changer le port dans `docker-compose.yml` **et** `DB_URL`. |
| Le serveur ne démarre pas : `Connection refused` | la base n'est pas prête : `docker compose ps` doit afficher `healthy`. |
| `Schema-validation: missing table/column` | base issue d'une ancienne version : `docker compose down -v && docker compose up -d db`, puis relancer le serveur. |
| `mvn` utilise la mauvaise version de Java | vérifier `JAVA_HOME` et `mvn -version` (Java 21 obligatoire). |
| Tests d'intégration « ignorés » | Docker n'est pas lancé : ouvrir Docker Desktop. |
| Le site affiche une erreur réseau | le serveur n'est pas lancé, ou n'écoute pas sur 8080. |
| Mobile : « serveur injoignable » | émulateur : `10.0.2.2`, téléphone réel : `--dart-define=API_URL=http://IP-du-PC:8080`, et pare-feu ouvert. |
| Mobile : erreur Gradle `flutter_secure_storage` | mettre `minSdk = 23`. |
| Compte verrouillé (5 échecs) | attendre, ou en dev : `update utilisateur set verrouille_jusqu_a = null where email = '...'`. |
| `flutter doctor` signale des licences | `flutter doctor --android-licenses`. |

---

## 9. Mise en production

Voir **`DEPLOIEMENT.md`** (variables, protections, hébergement) et **`deploiement/CHECKLIST_OUVERTURE.md`**
(étapes avant ouverture). Résumé :

```bash
cp deploiement/.env.example deploiement/.env      # puis remplir les secrets (openssl rand -base64 48)
docker compose -f deploiement/docker-compose.prod.yml --env-file deploiement/.env up -d --build
```

Prévoir un HTTPS devant le service `web` (port 8081), les sauvegardes quotidiennes (`deploiement/sauvegarde.sh`) et un
essai de restauration (`deploiement/restauration.sh`) avant l'ouverture. Pour l'application mobile en production :
`flutter build apk --dart-define=API_URL=https://ton-domaine` et retirer `usesCleartextTraffic`.

---

## 10. Pas encore fait

- Paiement réel **Orange Money** : l'adaptateur attend le contrat et les identifiants (en attendant :
  `PAIEMENT_FOURNISSEUR` vide en production, `simule` seulement en développement).
- Jetons stockés dans le navigateur et non dans des cookies `HttpOnly` (limite connue, voir `DEPLOIEMENT.md`).
