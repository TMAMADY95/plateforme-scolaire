import 'api_client.dart';
import 'modeles_messagerie.dart';

/// Les appels de la messagerie.
class ServiceMessagerie {
  ServiceMessagerie(this._api);

  final ApiClient _api;

  Future<List<Contact>> contacts(int eleveId) async {
    final json = await _api.get('/api/messagerie/contacts?eleveId=$eleveId');
    return json is List ? json.whereType<Map<String, dynamic>>().map(Contact.depuisJson).toList() : <Contact>[];
  }

  Future<List<Conversation>> conversations() async =>
      Conversation.listeDepuisJson(await _api.get('/api/messagerie/conversations'));

  Future<Conversation> demarrer({
    required int eleveId,
    required int destinataireId,
    required String sujet,
    required String contenu,
  }) async =>
      Conversation.depuisJson(await _api.post(
        '/api/messagerie/conversations',
        corps: {'eleveId': eleveId, 'destinataireId': destinataireId, 'sujet': sujet, 'contenu': contenu},
      ) as Map<String, dynamic>);

  /// Ouvre la conversation : le serveur la marque comme lue.
  Future<Conversation> ouvrir(int id) async =>
      Conversation.depuisJson(await _api.get('/api/messagerie/conversations/$id') as Map<String, dynamic>);

  Future<Conversation> repondre(int id, String contenu) async => Conversation.depuisJson(
        await _api.post('/api/messagerie/conversations/$id/messages', corps: {'contenu': contenu}) as Map<String, dynamic>,
      );

  /// Nombre de conversations non lues ; 0 si la réponse est inattendue.
  Future<int> nonLus() async {
    final json = await _api.get('/api/messagerie/non-lus');
    final valeur = json is Map<String, dynamic> ? json['nonLus'] : null;
    return valeur is num ? valeur.toInt() : 0;
  }
}
