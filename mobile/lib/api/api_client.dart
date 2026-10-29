import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;

/// Erreur renvoyée par l'API ou par le réseau, avec un message lisible par l'utilisateur.
class ApiException implements Exception {
  ApiException(this.statut, this.message);

  /// Code HTTP, ou 0 quand le serveur est injoignable.
  final int statut;
  final String message;

  @override
  String toString() => message;
}

/// Client HTTP minimal : ajoute le jeton, convertit les erreurs de l'API en [ApiException]
/// et prévient l'application quand la session n'est plus valide.
class ApiClient {
  ApiClient({required this.urlBase, http.Client? client, this.delai = const Duration(seconds: 20)})
      : _client = client ?? http.Client();

  final String urlBase;
  final Duration delai;
  final http.Client _client;

  String? _jeton;

  /// Le serveur a refusé le jeton (expiré, révoqué, compte verrouillé).
  void Function()? surNonAutorise;

  /// Le serveur exige le changement du mot de passe initial.
  void Function()? surMotDePasseRequis;

  void definirJeton(String? jeton) => _jeton = jeton;

  bool get aUnJeton => _jeton != null;

  Future<dynamic> get(String chemin) => _envoyer('GET', chemin);

  Future<dynamic> post(String chemin, {Object? corps, bool sansJeton = false}) =>
      _envoyer('POST', chemin, corps: corps, sansJeton: sansJeton);

  Future<dynamic> put(String chemin, {Object? corps}) => _envoyer('PUT', chemin, corps: corps);

  Future<dynamic> delete(String chemin) => _envoyer('DELETE', chemin);

  /// Télécharge un fichier protégé (PDF...) : le jeton part dans l'en-tête, pas dans l'adresse.
  Future<Uint8List> telecharger(String chemin) async => (await _executer('GET', chemin, accept: '*/*')).bodyBytes;

  Future<dynamic> _envoyer(String methode, String chemin, {Object? corps, bool sansJeton = false}) async {
    final reponse = await _executer(methode, chemin, corps: corps, sansJeton: sansJeton);
    final texte = utf8.decode(reponse.bodyBytes);
    if (reponse.statusCode == 204 || texte.isEmpty) return null;
    return _lireJson(texte);
  }

  /// Envoie la requête ; renvoie la réponse si elle est en 2xx, sinon lève une [ApiException].
  Future<http.Response> _executer(String methode, String chemin,
      {Object? corps, bool sansJeton = false, String accept = 'application/json'}) async {
    final avecJeton = _jeton != null && !sansJeton;
    final entetes = <String, String>{'Accept': accept};
    if (corps != null) entetes['Content-Type'] = 'application/json';
    if (avecJeton) entetes['Authorization'] = 'Bearer $_jeton';

    final requete = http.Request(methode, Uri.parse('$urlBase$chemin'))..headers.addAll(entetes);
    if (corps != null) requete.body = jsonEncode(corps);

    final http.Response reponse;
    try {
      reponse = await http.Response.fromStream(await _client.send(requete).timeout(delai));
    } on TimeoutException {
      throw ApiException(0, 'Le serveur ne répond pas. Réessayez dans un instant.');
    } catch (_) {
      throw ApiException(0, 'Le serveur est injoignable. Vérifiez votre connexion.');
    }

    if (reponse.statusCode >= 200 && reponse.statusCode < 300) return reponse;

    final erreur = _lireJson(utf8.decode(reponse.bodyBytes, allowMalformed: true));
    final corpsErreur = erreur is Map<String, dynamic> ? erreur : const <String, dynamic>{};
    if (reponse.statusCode == 401 && avecJeton) surNonAutorise?.call();
    if (reponse.statusCode == 403 && corpsErreur['erreur'] == 'CHANGEMENT_MOT_DE_PASSE_REQUIS') {
      surMotDePasseRequis?.call();
    }
    final message = corpsErreur['detail'] ?? corpsErreur['message'];
    throw ApiException(reponse.statusCode, message is String ? message : _messageParDefaut(reponse.statusCode));
  }

  static dynamic _lireJson(String texte) {
    try {
      return jsonDecode(texte);
    } catch (_) {
      return null;
    }
  }

  static String _messageParDefaut(int statut) {
    switch (statut) {
      case 401:
        return 'Session expirée, veuillez vous reconnecter.';
      case 403:
        return "Vous n'avez pas le droit d'effectuer cette action.";
      case 404:
        return 'Élément introuvable.';
      default:
        return 'Une erreur est survenue (code $statut).';
    }
  }
}
