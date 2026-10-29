import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/utils/format.dart';

void main() {
  test('formaterCoefficient : entier sans décimale, virgule française', () {
    expect(formaterCoefficient(3), '3');
    expect(formaterCoefficient(2.5), '2,5');
  });

  test('LigneBulletin lit le coefficient, 1 par défaut', () {
    expect(LigneBulletin.depuisJson({'matiere': 'Maths', 'coefficient': 3}).coefficient, 3);
    expect(LigneBulletin.depuisJson({'matiere': 'Maths'}).coefficient, 1);
  });
}
