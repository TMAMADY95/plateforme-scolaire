import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/modeles_resume.dart';
import 'package:gestion_scolaire/utils/format.dart';

void main() {
  test('le résumé se lit depuis le JSON du serveur', () {
    final liste = ResumeEnfant.listeDepuisJson([
      {
        'eleveId': 7,
        'classeId': 5,
        'periode': 'Trimestre 1',
        'moyenneGenerale': 14.5,
        'rang': 3,
        'effectif': 28,
        'absencesSemaine': 2,
        'absencesNonJustifiees': 1,
        'retardsSemaine': 0,
        'prochainsDevoirs': [
          {'id': 1, 'matiere': 'Maths', 'titre': 'Exercices', 'dateLimite': '2026-11-03'},
        ],
        'facturesImpayees': 1,
        'facturesEnRetard': 1,
        'resteAPayer': 12500,
        'devise': 'XOF',
      },
    ]);
    final r = liste.single;
    expect(r.moyenneGenerale, 14.5);
    expect(r.rang, 3);
    expect(r.prochainDevoir?.matiere, 'Maths');
    expect(r.resteAPayer, 12500);
  });

  test('sans total (devises différentes), le montant reste vide', () {
    final r = ResumeEnfant.listeDepuisJson([
      {
        'eleveId': 7,
        'facturesImpayees': 2,
        'facturesEnRetard': 0,
        'resteAPayer': null,
        'devise': null,
      },
    ]).single;
    expect(r.resteAPayer, isNull);
    expect(r.facturesImpayees, 2);
  });

  test('une réponse inattendue donne une liste vide', () {
    expect(ResumeEnfant.listeDepuisJson({'id': 1, 'email': 'a@b.c'}), isEmpty);
    expect(ResumeEnfant.listeDepuisJson(null), isEmpty);
    expect(ResumeEnfant.listeDepuisJson([{'sans': 'eleveId'}]), isEmpty);
  });

  test('résumé des présences', () {
    expect(resumePresences(0, 0, 0), 'Aucune absence');
    expect(resumePresences(1, 0, 0), '1 absence');
    expect(resumePresences(2, 1, 1), '2 absences (1 à justifier) · 1 retard');
    expect(resumePresences(0, 0, 3), '3 retards');
  });
}
