import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/auth/etat_authentification.dart';
import 'package:gestion_scolaire/auth/stockage_jeton.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

const moiParent = {
  'id': 7,
  'email': 'parent@example.org',
  'changementMotDePasseRequis': false,
  'affectations': [
    {'role': 'PARENT', 'perimetre': 'PERSONNEL', 'zoneId': null, 'etablissementId': null},
  ],
};

EtatAuthentification creer(MockClientHandler gestionnaire, StockageJetonMemoire stockage) {
  final api = ApiClient(urlBase: 'http://serveur', client: MockClient(gestionnaire));
  return EtatAuthentification(api: api, stockage: stockage);
}

void main() {
  test('sans jeton enregistré, on démarre déconnecté', () async {
    final etat = creer((_) async => json(500, {}), StockageJetonMemoire());

    await etat.demarrer();

    expect(etat.phase, Phase.deconnecte);
  });

  test('un jeton encore valide rouvre la session', () async {
    final etat = creer((_) async => json(200, moiParent), StockageJetonMemoire('jwt'));

    await etat.demarrer();

    expect(etat.phase, Phase.connecte);
    expect(etat.moi!.estParent, isTrue);
  });

  test('un jeton refusé par le serveur est effacé', () async {
    final stockage = StockageJetonMemoire('perime');
    final etat = creer((_) async => json(401, {'detail': 'Jeton invalide'}), stockage);

    await etat.demarrer();

    expect(etat.phase, Phase.deconnecte);
    expect(stockage.jeton, isNull);
  });

  test('un serveur injoignable au démarrage ne déconnecte pas : on propose de réessayer', () async {
    final stockage = StockageJetonMemoire('jwt');
    final etat = creer((_) async => throw http.ClientException('hors ligne'), stockage);

    await etat.demarrer();

    expect(etat.phase, Phase.horsLigne);
    expect(stockage.jeton, 'jwt');
  });

  test('la connexion enregistre le jeton et charge le profil', () async {
    final stockage = StockageJetonMemoire();
    final etat = creer((requete) async {
      if (requete.url.path == '/api/auth/login') {
        return json(200, {
          'token': 'nouveau',
          'type': 'Bearer',
          'utilisateurId': 7,
          'email': 'parent@example.org',
          'changementMotDePasseRequis': false,
        });
      }
      expect(requete.headers['Authorization'], 'Bearer nouveau');
      return json(200, moiParent);
    }, stockage);

    await etat.connexion(' parent@example.org ', 'secret-long-123');

    expect(etat.phase, Phase.connecte);
    expect(stockage.jeton, 'nouveau');
  });

  test('une connexion refusée lève une erreur lisible et ne change pas la session', () async {
    final stockage = StockageJetonMemoire();
    final etat = creer((_) async => json(401, {'detail': 'Identifiants invalides'}), stockage);

    await expectLater(
      etat.connexion('a@b.c', 'mauvais'),
      throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Identifiants invalides')),
    );
    expect(stockage.jeton, isNull);
    expect(etat.phase, isNot(Phase.connecte));
  });

  test('après un changement de mot de passe, la session est fermée avec un message', () async {
    final stockage = StockageJetonMemoire('jwt');
    final etat = creer((requete) async {
      if (requete.url.path == '/api/me/mot-de-passe') return http.Response('', 204);
      return json(200, moiParent);
    }, stockage);
    await etat.demarrer();

    await etat.changerMotDePasse('ancien-mot-de-passe', 'nouveau-mot-de-passe');

    expect(etat.phase, Phase.deconnecte);
    expect(stockage.jeton, isNull);
    expect(etat.message, contains('Mot de passe modifié'));
  });

  test('la session expirée en cours de route ramène à la connexion', () async {
    final stockage = StockageJetonMemoire('jwt');
    var premiere = true;
    final etat = creer((_) async {
      if (premiere) {
        premiere = false;
        return json(200, moiParent);
      }
      return json(401, {'detail': 'Jeton invalide'});
    }, stockage);
    await etat.demarrer();

    await expectLater(etat.api.get('/api/me/enfants'), throwsA(isA<ApiException>()));
    await Future<void>.delayed(Duration.zero);

    expect(etat.phase, Phase.deconnecte);
    expect(etat.message, contains('expiré'));
    expect(stockage.jeton, isNull);
  });
}
