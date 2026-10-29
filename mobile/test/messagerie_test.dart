import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/service_messagerie.dart';
import 'package:gestion_scolaire/ecrans/ecran_conversation.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

Map<String, Object?> conversation({List<Map<String, Object?>> messages = const [], bool active = true}) => {
      'id': 5,
      'eleveId': 7,
      'eleve': 'Fanta Diallo',
      'sujet': 'Devoirs',
      'interlocuteur': 'Moussa Ba',
      'roleInterlocuteur': "Personnel de l'école",
      'dernierMessageLe': '2026-10-29T10:00:00Z',
      'apercu': 'Bonjour',
      'nonLu': false,
      'active': active,
      'messages': messages,
    };

Map<String, Object?> message(int id, String contenu, {required bool moi}) => {
      'id': id,
      'auteurId': moi ? 1 : 2,
      'auteur': moi ? 'Awa Diallo' : 'Moussa Ba',
      'moi': moi,
      'contenu': contenu,
      'envoyeLe': '2026-10-29T10:00:00Z',
    };

void main() {
  test('le service appelle les bonnes URL et lit le nombre de non lus', () async {
    final urls = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        urls.add('${r.method} ${r.url}');
        if (r.url.path.endsWith('non-lus')) return json(200, {'nonLus': 3});
        if (r.url.path.endsWith('contacts')) {
          return json(200, [
            {'utilisateurId': 2, 'nom': 'Moussa Ba', 'role': 'Mathématiques'},
          ]);
        }
        return json(200, []);
      }),
    );
    final service = ServiceMessagerie(api);

    expect(await service.nonLus(), 3);
    final contacts = await service.contacts(7);
    expect(contacts.single.nom, 'Moussa Ba');
    await service.conversations();

    expect(urls, [
      'GET http://serveur/api/messagerie/non-lus',
      'GET http://serveur/api/messagerie/contacts?eleveId=7',
      'GET http://serveur/api/messagerie/conversations',
    ]);
  });

  test('une réponse inattendue donne 0 non lu', () async {
    final api = ApiClient(urlBase: 'http://serveur', client: MockClient((_) async => json(200, {'id': 1})));
    expect(await ServiceMessagerie(api).nonLus(), 0);
  });

  testWidgets('le fil affiche les messages et permet de répondre', (tester) async {
    var messages = [message(1, 'Bonjour Madame', moi: true), message(2, 'Bonjour, oui ?', moi: false)];
    String? corpsEnvoye;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        if (r.method == 'POST') {
          corpsEnvoye = r.body;
          messages = [...messages, message(3, 'Merci beaucoup', moi: true)];
        }
        return json(r.method == 'POST' ? 201 : 200, conversation(messages: messages));
      }),
    );

    await tester.pumpWidget(
      Provider<ServiceMessagerie>.value(
        value: ServiceMessagerie(api),
        child: const MaterialApp(home: EcranConversation(conversationId: 5)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Bonjour Madame'), findsOneWidget);
    expect(find.text('Bonjour, oui ?'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Merci beaucoup');
    await tester.tap(find.byTooltip('Envoyer'));
    await tester.pumpAndSettle();

    expect(jsonDecode(corpsEnvoye!), {'contenu': 'Merci beaucoup'});
    expect(find.text('Merci beaucoup'), findsOneWidget);
  });

  testWidgets('une conversation inactive ne propose plus de répondre', (tester) async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => json(200, conversation(active: false))),
    );
    await tester.pumpWidget(
      Provider<ServiceMessagerie>.value(
        value: ServiceMessagerie(api),
        child: const MaterialApp(home: EcranConversation(conversationId: 5)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(TextField), findsNothing);
    expect(find.textContaining("n'est plus active"), findsOneWidget);
  });
}
