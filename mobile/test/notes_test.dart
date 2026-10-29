import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/modeles_enseignant.dart';
import 'package:gestion_scolaire/api/service_enseignant.dart';
import 'package:gestion_scolaire/ecrans/onglet_notes.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

Map<String, Object?> evaluationJson() => {
      'id': 5,
      'classeId': 3,
      'matiereId': 1,
      'matiere': 'Maths',
      'periodeId': 1,
      'periode': 'Trimestre 1',
      'enseignantId': 2,
      'titre': 'Devoir 1',
      'dateEvaluation': '2026-10-20',
      'coefficient': 2,
      'bareme': 20,
    };

Map<String, Object?> ligneNote(int id, String nom, num? valeur, {bool absent = false}) => {
      'inscriptionId': id,
      'eleve': {'id': id, 'matricule': 'M-$id', 'nom': nom, 'prenom': 'Test'},
      'valeur': valeur,
      'absent': absent,
    };

void main() {
  test('lireNote accepte la virgule, refuse hors barème', () {
    expect(lireNote('14,5', 20), 14.5);
    expect(lireNote(' ', 20), isNull);
    expect(lireNote('20', 20), 20);
    expect(() => lireNote('21', 20), throwsFormatException);
    expect(() => lireNote('abc', 20), throwsFormatException);
    expect(() => lireNote('-1', 20), throwsFormatException);
  });

  test('SaisieNote : valeur absente quand élève absent', () {
    expect(const SaisieNote(inscriptionId: 1, absent: true).versJson(), {'inscriptionId': 1, 'absent': true});
    expect(const SaisieNote(inscriptionId: 1, valeur: 12.5).versJson(), {
      'inscriptionId': 1,
      'valeur': 12.5,
      'absent': false,
    });
  });

  testWidgets('notes : liste, grille puis enregistrement', (tester) async {
    final requetes = <String>[];
    Map<String, dynamic>? corps;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        requetes.add('${r.method} ${r.url.path}');
        if (r.url.path == '/api/classes/3/evaluations') return json(200, [evaluationJson()]);
        final grille = {
          'evaluation': evaluationJson(),
          'lignes': [ligneNote(11, 'Diallo', null), ligneNote(12, 'Sow', 9.5)],
        };
        if (r.method == 'PUT') corps = jsonDecode(r.body) as Map<String, dynamic>;
        return json(200, grille);
      }),
    );
    final classe = ClasseEnseignee.grouper([
      AffectationEnseignant.depuisJson({
        'classeId': 3,
        'classeNom': '6e A',
        'niveauLibelle': '6e',
        'anneeScolaireId': 1,
        'anneeLibelle': '1',
        'matiereLibelle': 'Maths',
      }),
    ]).first;
    await tester.pumpWidget(
      Provider<ServiceEnseignant>.value(
        value: ServiceEnseignant(api),
        child: MaterialApp(home: Scaffold(body: OngletNotes(classe: classe))),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Devoir 1'), findsOneWidget);

    await tester.tap(find.text('Devoir 1'));
    await tester.pumpAndSettle();
    expect(find.text('Diallo Test'), findsOneWidget);

    await tester.enterText(find.byKey(const ValueKey('note-11')), '14,5');
    await tester.tap(find.byKey(const ValueKey('absent-12')));
    await tester.pump();
    await tester.tap(find.text('Enregistrer les notes'));
    await tester.pumpAndSettle();

    expect(requetes, contains('PUT /api/evaluations/5/notes'));
    final lignes = (corps!['lignes'] as List<dynamic>).cast<Map<String, dynamic>>();
    expect(lignes[0], {'inscriptionId': 11, 'valeur': 14.5, 'absent': false});
    expect(lignes[1], {'inscriptionId': 12, 'absent': true});
    expect(find.text('Notes enregistrées.'), findsOneWidget);
  });

  testWidgets('notes : une note hors barème n\'est pas envoyée', (tester) async {
    var envois = 0;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        if (r.method == 'PUT') envois++;
        return json(200, {
          'evaluation': evaluationJson(),
          'lignes': [ligneNote(11, 'Diallo', null)],
        });
      }),
    );
    await tester.pumpWidget(
      Provider<ServiceEnseignant>.value(
        value: ServiceEnseignant(api),
        child: MaterialApp(
          home: EcranGrilleNotes(evaluation: EvaluationClasse.depuisJson(evaluationJson())),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('note-11')), '25');
    await tester.tap(find.text('Enregistrer les notes'));
    await tester.pump();
    expect(find.textContaining('entre 0 et 20'), findsOneWidget);
    expect(envois, 0);
  });
}
