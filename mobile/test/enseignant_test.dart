import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/modeles.dart';
import 'package:gestion_scolaire/api/modeles_enseignant.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/api/service_enseignant.dart';
import 'package:gestion_scolaire/ecrans/ecran_classe.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

Map<String, Object?> affectation(int classeId, String nom, int annee, String matiere) => {
      'classeId': classeId,
      'classeNom': nom,
      'niveauLibelle': '6e',
      'anneeScolaireId': annee,
      'anneeLibelle': '$annee',
      'etablissementId': 1,
      'matiereId': 1,
      'matiereLibelle': matiere,
    };

Map<String, Object?> ligneFeuille(int id, String nom, String? statut) => {
      'inscriptionId': id,
      'eleve': {'id': id, 'matricule': 'M-$id', 'nom': nom, 'prenom': 'Test', 'dateNaissance': '2012-01-01'},
      'statut': statut,
      'motif': null,
      'justifie': false,
    };

void main() {
  test('le rôle enseignant est reconnu et utilise l\'application mobile', () {
    final moi = Moi.depuisJson({
      'id': 1,
      'email': 'prof@example.org',
      'changementMotDePasseRequis': false,
      'affectations': [
        {'role': 'ENSEIGNANT', 'perimetre': 'ETABLISSEMENT', 'etablissementId': 1},
      ],
    });
    expect(moi.estEnseignant, isTrue);
    expect(moi.estUtilisateurMobile, isTrue);
  });

  test('les affectations sont regroupées par classe, années récentes d\'abord', () {
    final classes = ClasseEnseignee.grouper([
      for (final a in [
        affectation(1, '6e B', 1, 'Maths'),
        affectation(2, '5e A', 2, 'Maths'),
        affectation(2, '5e A', 2, 'Physique'),
        affectation(2, '5e A', 2, 'Maths'),
      ])
        AffectationEnseignant.depuisJson(a),
    ]);

    expect(classes.map((c) => c.nom), ['5e A', '6e B']);
    expect(classes.first.matieres, ['Maths', 'Physique']);
  });

  test('une ligne d\'appel n\'envoie motif et justification que pour une absence ou un retard', () {
    expect(
      const LigneAppel(inscriptionId: 1, statut: StatutPresence.present, motif: 'ignoré', justifie: true).versJson(),
      {'inscriptionId': 1, 'statut': 'PRESENT', 'justifie': false},
    );
    expect(
      const LigneAppel(inscriptionId: 2, statut: StatutPresence.absent, motif: '  Malade ', justifie: true).versJson(),
      {'inscriptionId': 2, 'statut': 'ABSENT', 'motif': 'Malade', 'justifie': true},
    );
  });

  testWidgets('appel : tout le monde présent puis enregistrement', (tester) async {
    final requetes = <String>[];
    Map<String, dynamic>? corps;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        requetes.add('${r.method} ${r.url.path}');
        if (r.method == 'PUT') {
          corps = jsonDecode(r.body) as Map<String, dynamic>;
          return json(200, {
            'classeId': 3,
            'date': '2026-10-29',
            'lignes': [ligneFeuille(11, 'Diallo', 'PRESENT'), ligneFeuille(12, 'Sow', 'PRESENT')],
          });
        }
        return json(200, {
          'classeId': 3,
          'date': '2026-10-29',
          'lignes': [ligneFeuille(11, 'Diallo', null), ligneFeuille(12, 'Sow', null)],
        });
      }),
    );
    final classe = ClasseEnseignee.grouper([AffectationEnseignant.depuisJson(affectation(3, '6e A', 1, 'Maths'))]).first;
    await tester.pumpWidget(
      Provider<ServiceEnseignant>.value(
        value: ServiceEnseignant(api),
        child: MaterialApp(
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          supportedLocales: const [Locale('fr')],
          home: EcranClasse(classe: classe, maintenant: DateTime(2026, 10, 29)),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Diallo Test'), findsOneWidget);
    expect(find.textContaining('2 non renseigné(s)'), findsOneWidget);

    await tester.tap(find.text('Tout le monde présent'));
    await tester.pump();
    expect(find.textContaining('2 présent(s)'), findsOneWidget);

    await tester.tap(find.text("Enregistrer l'appel"));
    await tester.pumpAndSettle();

    expect(requetes, contains('PUT /api/classes/3/presences/2026-10-29'));
    final lignes = corps!['lignes'] as List<dynamic>;
    expect(lignes.map((l) => (l as Map<String, dynamic>)['statut']), ['PRESENT', 'PRESENT']);
    expect(find.text('Appel enregistré.'), findsOneWidget);
  });

  testWidgets('appel : enregistrer sans aucun statut est refusé côté application', (tester) async {
    var envois = 0;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        if (r.method == 'PUT') envois++;
        return json(200, {
          'classeId': 3,
          'date': '2026-10-29',
          'lignes': [ligneFeuille(11, 'Diallo', null)],
        });
      }),
    );
    final classe = ClasseEnseignee.grouper([AffectationEnseignant.depuisJson(affectation(3, '6e A', 1, 'Maths'))]).first;
    await tester.pumpWidget(
      Provider<ServiceEnseignant>.value(
        value: ServiceEnseignant(api),
        child: MaterialApp(
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          supportedLocales: const [Locale('fr')],
          home: EcranClasse(classe: classe, maintenant: DateTime(2026, 10, 29)),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text("Enregistrer l'appel"));
    await tester.pump();

    expect(find.text("Indiquez au moins un statut avant d'enregistrer."), findsOneWidget);
    expect(envois, 0);
  });
}
