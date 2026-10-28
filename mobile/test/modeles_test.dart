import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/modeles.dart';

void main() {
  test('Moi lit les affectations et ignore les rôles inconnus', () {
    final moi = Moi.depuisJson({
      'id': 7,
      'email': 'parent@example.org',
      'changementMotDePasseRequis': false,
      'affectations': [
        {'role': 'PARENT', 'perimetre': 'PERSONNEL', 'zoneId': null, 'etablissementId': null},
        {'role': 'ROLE_FUTUR', 'perimetre': 'NATIONAL', 'zoneId': null, 'etablissementId': null},
      ],
    });

    expect(moi.id, 7);
    expect(moi.estParent, isTrue);
    expect(moi.estEleve, isFalse);
    expect(moi.affectations, hasLength(1));
    expect(moi.estUtilisateurMobile, isTrue);
  });

  test("un rôle d'établissement n'est pas un utilisateur mobile", () {
    final moi = Moi.depuisJson({
      'id': 1,
      'email': 'direction@example.org',
      'changementMotDePasseRequis': true,
      'affectations': [
        {'role': 'DIRECTION_ETABLISSEMENT', 'perimetre': 'ETABLISSEMENT', 'zoneId': null, 'etablissementId': 3},
      ],
    });

    expect(moi.estUtilisateurMobile, isFalse);
    expect(moi.changementMotDePasseRequis, isTrue);
    expect(moi.affectations.single.etablissementId, 3);
  });

  test('ReponseConnexion lit le jeton', () {
    final reponse = ReponseConnexion.depuisJson({
      'token': 'jwt',
      'type': 'Bearer',
      'utilisateurId': 1,
      'email': 'a@b.c',
      'changementMotDePasseRequis': true,
    });

    expect(reponse.jeton, 'jwt');
    expect(reponse.changementMotDePasseRequis, isTrue);
  });
}
