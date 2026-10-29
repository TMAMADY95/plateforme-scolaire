# Étape 3 — Factures et paiement en ligne

Nouveau : `lib/ecrans/onglet_factures.dart`. Modifiés : `pubspec.yaml` (ajout de `url_launcher`),
`lib/api/modeles_scolaires.dart`, `lib/api/service_scolaire.dart`, `lib/utils/format.dart`,
`lib/ecrans/ecran_enfant.dart`, `test/scolaire_test.dart`.

1. `flutter pub get`  2. `flutter analyze`  3. `flutter test`

Paiement : le bouton « Payer » ouvre la page du prestataire dans le navigateur. En développement c'est la page
simulée du site web (`/paiement-simule/...`, adresse définie par le backend) : il faut y être connecté au site,
puis cliquer « Simuler un succès » et revenir dans l'application ; la liste se recharge au retour (ou bouton Actualiser).
