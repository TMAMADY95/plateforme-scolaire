# Application mobile (Flutter)

Application des parents et des élèves. Étape 1 : socle (connexion, session, changement de mot de passe).

## Mise en place (une seule fois)

Les dossiers `android/` et `ios/` sont générés par Flutter, pas fournis ici. Depuis la racine du projet :

```bash
flutter create --org com.plateforme --project-name gestion_scolaire --platforms android,ios mobile_tmp
cp -r mobile_tmp/android mobile_tmp/ios mobile/
rm -rf mobile_tmp
cd mobile
flutter pub get
```

Les fichiers `pubspec.yaml`, `analysis_options.yaml`, `lib/` et `test/` fournis remplacent ceux de Flutter.

### Réglages Android (développement)

Dans `android/app/src/main/AndroidManifest.xml`, sur la balise `<application ...>` :

```xml
android:usesCleartextTraffic="true"
```

Cela autorise le HTTP non chiffré vers votre poste en développement. **À retirer en production** (HTTPS obligatoire).

Dans `android/app/build.gradle(.kts)`, si Gradle se plaint de `flutter_secure_storage` : `minSdk = 23`.

## Lancer

Le backend doit tourner (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`).

```bash
# Émulateur Android (10.0.2.2 = le poste de développement)
flutter run

# Téléphone réel : adresse IP du poste sur le réseau local
flutter run --dart-define=API_URL=http://192.168.1.20:8080

# Simulateur iOS
flutter run --dart-define=API_URL=http://localhost:8080
```

Comptes de test (profil dev, mot de passe `Demo!2026` sauf si `DEV_PASSWORD` est défini) :

- élève : `eleve1@example.org` ;
- parent : aucun compte de démonstration. Créez-en un depuis le site web (menu Élèves > Parents d'un élève) : le compte reçoit un mot de passe initial et l'application demandera de le changer à la première connexion.

Les autres rôles (direction, ministère, enseignants) se connectent mais sont renvoyés vers le site web.

## Tests

```bash
flutter analyze
flutter test
```
