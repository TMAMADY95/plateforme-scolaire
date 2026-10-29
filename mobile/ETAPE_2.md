# Étape 2 — Mes enfants / Mon espace

Nouveaux fichiers : `lib/api/modeles_scolaires.dart`, `lib/api/service_scolaire.dart`, `lib/utils/format.dart`,
`lib/widgets/chargeur_donnees.dart`, `lib/ecrans/{ecran_enfant,onglet_bulletin,onglet_presences,onglet_emploi_du_temps}.dart`,
`test/scolaire_test.dart`. Modifiés : `lib/app.dart`, `lib/ecrans/ecran_accueil.dart`, `test/ecrans_test.dart`.

Vérifier : `flutter analyze` puis `flutter test`.
Essayer : `flutter run -d chrome --web-port=5000 --dart-define=API_URL=http://localhost:8080`
(`eleve1@example.org` / `Demo!2026` ; pour un parent, créer le compte dans le web : Élèves > Parents).
