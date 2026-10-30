import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/ecrans/onglet_annonces.dart';

void main() {
  test('les annonces se lisent depuis le JSON', () {
    final liste = Annonce.listeDepuisJson([
      {'id': 1, 'auteur': 'Moussa Ba', 'titre': 'Sortie', 'contenu': 'Vendredi', 'publieLe': '2026-10-30T10:00:00Z'},
    ]);
    expect(liste.single.titre, 'Sortie');
    expect(Annonce.listeDepuisJson(null), isEmpty);
  });

  testWidgets('l\'onglet affiche les annonces', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletAnnonces(
            charger: () async => const [
              Annonce(id: 1, auteur: 'Moussa Ba', titre: 'Sortie scolaire', contenu: 'Vendredi à 8h', publieLe: '2026-10-30T10:00:00Z'),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Sortie scolaire'), findsOneWidget);
    expect(find.text('Vendredi à 8h'), findsOneWidget);
  });

  testWidgets('sans annonce, un message est affiché', (tester) async {
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: OngletAnnonces(charger: () async => const []))));
    await tester.pumpAndSettle();
    expect(find.text('Aucune annonce pour le moment.'), findsOneWidget);
  });
}
