import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/modeles_enseignant.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/api/service_enseignant.dart';
import 'package:gestion_scolaire/ecrans/onglet_annonces.dart';
import 'package:gestion_scolaire/ecrans/onglet_devoirs.dart';

void main() {
  test('le service envoie les publications et les suppressions', () async {
    final appels = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        appels.add('${r.method} ${r.url.path} ${r.body}');
        return http.Response('', 204);
      }),
    );
    final service = ServiceEnseignant(api);
    await service.publierDevoir(3, matiereId: 7, titre: 'Ex', consigne: '', dateLimite: '2026-11-05');
    await service.publierAnnonce(3, titre: 'Sortie', contenu: 'Vendredi');
    await service.supprimerDevoir(9);
    await service.supprimerAnnonce(4);

    expect(jsonDecode(appels[0].substring(appels[0].indexOf('{'))), {'matiereId': 7, 'titre': 'Ex', 'dateLimite': '2026-11-05'});
    expect(appels[0], startsWith('POST /api/classes/3/devoirs'));
    expect(appels[1], startsWith('POST /api/classes/3/annonces'));
    expect(appels[2].trim(), 'DELETE /api/devoirs/9');
    expect(appels[3].trim(), 'DELETE /api/annonces/4');
  });

  test('les matières de l\'enseignant gardent leur identifiant', () {
    final classes = ClasseEnseignee.grouper([
      const AffectationEnseignant(
          classeId: 1, classeNom: '6A', niveau: '6e', anneeScolaireId: 1, anneeLibelle: '2026', matiere: 'Maths', matiereId: 7),
      const AffectationEnseignant(
          classeId: 1, classeNom: '6A', niveau: '6e', anneeScolaireId: 1, anneeLibelle: '2026', matiere: 'Physique', matiereId: 8),
    ]);
    expect(classes.single.matieresAvecId.map((m) => m.id), [7, 8]);
  });

  testWidgets('publier une annonce recharge la liste', (tester) async {
    final publiees = <String>[];
    var liste = <Annonce>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletAnnonces(
            charger: () async => liste,
            publier: (titre, contenu) async {
              publiees.add('$titre|$contenu');
              liste = [Annonce(id: 1, auteur: 'Moi', titre: titre, contenu: contenu, publieLe: '2026-10-30T10:00:00Z', modifiable: true)];
            },
            supprimer: (a) async {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Nouvelle annonce'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Publier'));
    await tester.pumpAndSettle();
    expect(find.text('Le titre est obligatoire.'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextFormField, 'Titre'), 'Sortie');
    await tester.enterText(find.widgetWithText(TextFormField, 'Message'), 'Vendredi à 8h');
    await tester.tap(find.text('Publier'));
    await tester.pumpAndSettle();

    expect(publiees, ['Sortie|Vendredi à 8h']);
    expect(find.text('Vendredi à 8h'), findsOneWidget);
    expect(find.byTooltip('Supprimer l\'annonce'), findsOneWidget);
  });

  testWidgets('seul le propriétaire voit le bouton de suppression d\'une annonce', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletAnnonces(
            charger: () async => const [Annonce(id: 1, auteur: 'Autre', titre: 'T', contenu: 'C', publieLe: '2026-10-30T10:00:00Z')],
            publier: (t, c) async {},
            supprimer: (a) async {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byTooltip('Supprimer l\'annonce'), findsNothing);
  });

  testWidgets('publier un devoir avec une seule matière', (tester) async {
    final envoyes = <String>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletDevoirs(
            maintenant: DateTime(2026, 11, 2),
            charger: () async => const [],
            matieres: const [MatiereEnseignee(7, 'Maths')],
            publier: (m, t, c, d) async => envoyes.add('$m|$t|$c|$d'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Nouveau devoir'));
    await tester.pumpAndSettle();
    expect(find.text('Matière : Maths'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextFormField, 'Titre'), 'Exercices');
    await tester.tap(find.text('Publier'));
    await tester.pumpAndSettle();

    expect(envoyes, ['7|Exercices||2026-11-03']);
  });

  testWidgets('supprimer un devoir demande confirmation', (tester) async {
    final supprimes = <int>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletDevoirs(
            charger: () async => const [
              Devoir(id: 4, matiereId: 7, matiere: 'Maths', enseignant: 'Moi', titre: 'Exercices', dateLimite: '2026-11-03'),
              Devoir(id: 5, matiereId: 9, matiere: 'Histoire', enseignant: 'Autre', titre: 'Leçon', dateLimite: '2026-11-03'),
            ],
            matieres: const [MatiereEnseignee(7, 'Maths')],
            publier: (m, t, c, d) async {},
            supprimer: (d) async => supprimes.add(d.id),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byTooltip('Supprimer le devoir'), findsOneWidget);
    await tester.tap(find.byTooltip('Supprimer le devoir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Supprimer'));
    await tester.pumpAndSettle();

    expect(supprimes, [4]);
  });

  testWidgets('une erreur du serveur est affichée', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletAnnonces(
            charger: () async => const [],
            publier: (t, c) async => throw ApiException(403, 'Interdit.'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Nouvelle annonce'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Titre'), 'A');
    await tester.enterText(find.widgetWithText(TextFormField, 'Message'), 'B');
    await tester.tap(find.text('Publier'));
    await tester.pumpAndSettle();
    expect(find.text('Interdit.'), findsOneWidget);
  });
}
