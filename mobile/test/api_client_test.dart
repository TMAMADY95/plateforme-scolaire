import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

void main() {
  test("ajoute le jeton dans l'en-tête Authorization", () async {
    late http.BaseRequest recue;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((requete) async {
        recue = requete;
        return json(200, {'ok': true});
      }),
    )..definirJeton('abc');

    final reponse = await api.get('/api/me');

    expect(reponse, {'ok': true});
    expect(recue.headers['Authorization'], 'Bearer abc');
    expect(recue.url.toString(), 'http://serveur/api/me');
  });

  test("n'envoie pas de jeton quand l'appel est sans jeton", () async {
    late http.BaseRequest recue;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((requete) async {
        recue = requete;
        return json(200, {});
      }),
    )..definirJeton('abc');

    await api.post('/api/auth/login', corps: {'email': 'a@b.c'}, sansJeton: true);

    expect(recue.headers.containsKey('Authorization'), isFalse);
    expect(recue.headers['Content-Type'], contains('application/json'));
  });

  test('lit le message détaillé des erreurs, accents compris', () async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => json(409, {'detail': 'Cette adresse e-mail est déjà utilisée'})),
    );

    expect(
      () => api.post('/x', corps: {}),
      throwsA(isA<ApiException>()
          .having((e) => e.statut, 'statut', 409)
          .having((e) => e.message, 'message', 'Cette adresse e-mail est déjà utilisée')),
    );
  });

  test('un 401 avec jeton prévient la session ; sans jeton (connexion) non', () async {
    var appels = 0;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => json(401, {'detail': 'Identifiants invalides'})),
    )..surNonAutorise = () => appels++;

    await expectLater(api.post('/api/auth/login', corps: {}, sansJeton: true), throwsA(isA<ApiException>()));
    expect(appels, 0);

    api.definirJeton('abc');
    await expectLater(api.get('/api/me'), throwsA(isA<ApiException>()));
    expect(appels, 1);
  });

  test('le changement de mot de passe exigé est signalé', () async {
    var signale = false;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => json(403, {'erreur': 'CHANGEMENT_MOT_DE_PASSE_REQUIS'})),
    )
      ..definirJeton('abc')
      ..surMotDePasseRequis = () => signale = true;

    await expectLater(api.get('/api/classes'), throwsA(isA<ApiException>()));
    expect(signale, isTrue);
  });

  test('un 204 renvoie null', () async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => http.Response('', 204)),
    );

    expect(await api.post('/api/me/mot-de-passe', corps: {}), isNull);
  });

  test('un serveur injoignable donne une erreur de statut 0', () async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => throw http.ClientException('connexion refusée')),
    );

    expect(
      () => api.get('/api/me'),
      throwsA(isA<ApiException>().having((e) => e.statut, 'statut', 0)),
    );
  });
}
