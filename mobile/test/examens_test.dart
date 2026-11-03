import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/ecrans/onglet_examens.dart';

void main() {
  test('le résultat d\'un admis avec mention est lisible', () {
    final r = ResultatExamen.depuisJson({
      'examen': 'BAC 2026',
      'anneeScolaire': '2025-2026',
      'dateProclamation': '2026-07-20',
      'numero': 'EX1-000001',
      'statut': 'PRESENT',
      'decision': 'ADMIS',
      'moyenne': 14.5,
      'mention': 'BIEN',
      'notes': [
        {'epreuve': 'Maths', 'coefficient': 3, 'note': 14.5},
      ],
    });
    expect(r.libelleResultat, 'Admis(e) : Bien');
    expect(r.moyenne, 14.5);
    expect(r.notes.single.coefficient, 3);
  });

  test('absent, éliminé et non admis', () {
    expect(ResultatExamen.depuisJson({'examen': 'CEE', 'statut': 'ABSENT'}).libelleResultat, 'Absent(e)');
    expect(ResultatExamen.depuisJson({'examen': 'CEE', 'statut': 'ELIMINE', 'decision': 'NON_ADMIS'}).libelleResultat, 'Éliminé(e)');
    expect(ResultatExamen.depuisJson({'examen': 'CEE', 'statut': 'PRESENT', 'decision': 'NON_ADMIS'}).libelleResultat, 'Non admis(e)');
  });

  testWidgets('la liste vide explique qu\'aucun résultat n\'est proclamé', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: ListeResultatsExamens(resultats: []))));
    expect(find.textContaining('Aucun résultat'), findsOneWidget);
  });

  testWidgets('un résultat affiche l\'examen, la mention et les notes', (tester) async {
    final r = ResultatExamen.depuisJson({
      'examen': 'BEPC 2026',
      'anneeScolaire': '2025-2026',
      'statut': 'PRESENT',
      'decision': 'ADMIS',
      'moyenne': 12.0,
      'mention': 'ASSEZ_BIEN',
      'notes': [
        {'epreuve': 'Français', 'coefficient': 2, 'note': 12.0},
      ],
    });
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: ListeResultatsExamens(resultats: [r]))));
    expect(find.text('BEPC 2026 (2025-2026)'), findsOneWidget);
    expect(find.text('Admis(e) : Assez bien'), findsOneWidget);
    expect(find.text('Français'), findsOneWidget);
  });

  test('le barème du primaire est lu (10), 20 par défaut', () {
    expect(ResultatExamen.depuisJson({'examen': 'CEE', 'statut': 'PRESENT', 'bareme': 10}).bareme, 10);
    expect(ResultatExamen.depuisJson({'examen': 'BAC', 'statut': 'PRESENT'}).bareme, 20);
  });
}
