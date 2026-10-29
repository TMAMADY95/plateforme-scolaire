# Étape 4 — Enseignants : mes classes et appel

Un enseignant voit « Mes classes » (regroupées par classe, années récentes d'abord) ; un appui ouvre la feuille
d'appel : date (aujourd'hui par défaut, jamais dans le futur), un statut par élève, motif et « justifié » pour une
absence ou un retard, « Tout le monde présent », enregistrement en une fois. Un compte parent + enseignant voit les deux sections.

Nouveaux : `lib/api/modeles_enseignant.dart`, `lib/api/service_enseignant.dart`, `lib/ecrans/{ecran_classe,onglet_appel}.dart`,
`test/enseignant_test.dart`. Modifiés : `pubspec.yaml` (flutter_localizations : calendrier en français), `lib/app.dart`,
`lib/api/modeles.dart`, `lib/ecrans/ecran_accueil.dart`, `test/ecrans_test.dart`.

`flutter pub get`, `flutter analyze`, `flutter test`. Essai : `prof1@example.org` / `Demo!2026`.
La saisie des notes viendra à l'étape 5.
