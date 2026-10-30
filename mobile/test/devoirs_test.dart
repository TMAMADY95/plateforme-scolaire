import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/api/service_scolaire.dart';
import 'package:gestion_scolaire/ecrans/onglet_devoirs.dart';

void main() {
  test('le service lit les devoirs de la classe', () async {
    final urls = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        urls.add(r.url.toString());
        return http.Response(
          jsonEncode([
            {
              'id': 1,
              'matiere': 'Maths',
              'enseignant': 'Awa Diallo',
              'titre': 'Exercices 1 à 5',
              'consigne': 'Page 42',
              'dateLimite': '2026-11-03',
            },
            {'id': 2, 'matiere': 'Français', 'enseignant': 'Moussa Ba', 'titre': 'Lecture', 'consigne': null, 'dateLimite': '2026-11-04'},
          ]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
    );
    final devoirs = await ServiceScolaire(api).devoirs(3);
    expect(urls, ['http://serveur/api/classes/3/devoirs']);
    expect(devoirs.length, 2);
    expect(devoirs.first.titre, 'Exercices 1 à 5');
    expect(devoirs.last.consigne, isNull);
  });

  testWidgets('l\'onglet affiche les devoirs et leur date limite', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletDevoirs(
            maintenant: DateTime(2026, 10, 29),
            charger: () async => const [
              Devoir(id: 1, matiere: 'Maths', enseignant: 'Awa Diallo', titre: 'Exercices 1 à 5', consigne: 'Page 42', dateLimite: '2026-11-03'),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Exercices 1 à 5'), findsOneWidget);
    expect(find.text('Page 42'), findsOneWidget);
    expect(find.text('Pour le 03/11/2026'), findsOneWidget);
    expect(find.text('Maths · Awa Diallo'), findsOneWidget);
  });

  testWidgets('sans devoir, un message est affiché', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: Scaffold(body: OngletDevoirs(charger: () async => const []))),
    );
    await tester.pumpAndSettle();
    expect(find.text('Aucun devoir à venir.'), findsOneWidget);
  });

  testWidgets('cocher un devoir prévient le serveur et barre le titre', (tester) async {
    final appels = <String>[];
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletDevoirs(
            maintenant: DateTime(2026, 10, 29),
            charger: () async => const [
              Devoir(id: 4, matiere: 'Maths', enseignant: 'Awa Diallo', titre: 'Exercices', dateLimite: '2026-11-03', fait: false),
            ],
            surBascule: (d, fait) async => appels.add('${d.id}:$fait'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byType(Checkbox));
    await tester.pumpAndSettle();

    expect(appels, ['4:true']);
    expect(find.text('Fait'), findsOneWidget);
  });

  testWidgets('la progression est visible pour l\'enseignant', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: OngletDevoirs(
            charger: () async => const [
              Devoir(id: 4, matiere: 'Maths', enseignant: 'Awa', titre: 'Exercices', dateLimite: '2026-11-03', faits: 12, effectif: 28),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('12/28 élèves ont terminé'), findsOneWidget);
    expect(find.byType(Checkbox), findsNothing);
  });
}
