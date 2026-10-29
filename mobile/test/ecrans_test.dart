import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/app.dart';
import 'package:gestion_scolaire/auth/etat_authentification.dart';
import 'package:gestion_scolaire/auth/stockage_jeton.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

Map<String, Object?> moi({required String role, bool changement = false}) => {
      'id': 1,
      'email': 'test@example.org',
      'changementMotDePasseRequis': changement,
      'affectations': [
        {'role': role, 'perimetre': 'PERSONNEL', 'zoneId': null, 'etablissementId': null},
      ],
    };

Future<void> afficher(WidgetTester tester, {String? jeton, Map<String, Object?>? profil}) async {
  final api = ApiClient(
    urlBase: 'http://serveur',
    client: MockClient((requete) async {
      if (profil == null) return json(500, {});
      if (requete.url.path == '/api/me/enfants') return json(200, []);
      return json(200, profil);
    }),
  );
  final etat = EtatAuthentification(api: api, stockage: StockageJetonMemoire(jeton));
  await etat.demarrer();
  await tester.pumpWidget(
    ChangeNotifierProvider<EtatAuthentification>.value(value: etat, child: const Application()),
  );
}

void main() {
  testWidgets('sans session, on voit la connexion', (tester) async {
    await afficher(tester);

    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
  });

  testWidgets('la connexion refuse une adresse e-mail invalide', (tester) async {
    await afficher(tester);

    await tester.enterText(find.byType(TextFormField).first, 'pas-un-email');
    await tester.tap(find.text('Se connecter'));
    await tester.pump();

    expect(find.text('Adresse e-mail invalide'), findsOneWidget);
  });

  testWidgets('un mot de passe initial impose son changement avant tout', (tester) async {
    await afficher(tester, jeton: 'jwt', profil: moi(role: 'PARENT', changement: true));

    expect(find.text('Changer le mot de passe'), findsOneWidget);
    expect(find.text('Enregistrer'), findsOneWidget);
  });

  testWidgets('un parent connecté arrive sur l\'accueil', (tester) async {
    await afficher(tester, jeton: 'jwt', profil: moi(role: 'PARENT'));

    expect(find.text('test@example.org'), findsOneWidget);
    expect(find.text('Parent'), findsOneWidget);
    await tester.pumpAndSettle();

    expect(find.text('Aucun enfant rattaché'), findsOneWidget);
  });

  testWidgets('un rôle hors parents et élèves est renvoyé vers le site web', (tester) async {
    await afficher(tester, jeton: 'jwt', profil: moi(role: 'MINISTERE'));

    expect(find.text('Application réservée aux familles et aux enseignants'), findsOneWidget);
  });

  testWidgets('mot de passe oublié : envoie la demande sans jeton et confirme sans rien révéler', (tester) async {
    final requetes = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        requetes.add('${r.method} ${r.url.path} ${r.body} ${r.headers['Authorization']}');
        return http.Response('', 204);
      }),
    );
    final etat = EtatAuthentification(api: api, stockage: StockageJetonMemoire(null));
    await etat.demarrer();
    await tester.pumpWidget(ChangeNotifierProvider<EtatAuthentification>.value(value: etat, child: const Application()));

    await tester.tap(find.text('Mot de passe oublié ?'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField), 'awa@example.org');
    await tester.tap(find.text('Envoyer le lien'));
    await tester.pumpAndSettle();

    expect(requetes, ['POST /api/auth/mot-de-passe-oublie {"email":"awa@example.org"} null']);
    expect(find.textContaining('Si cette adresse correspond à un compte'), findsOneWidget);
  });
}
