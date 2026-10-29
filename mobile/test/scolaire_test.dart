import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestion_scolaire/api/api_client.dart';
import 'package:gestion_scolaire/api/modeles_scolaires.dart';
import 'package:gestion_scolaire/api/service_scolaire.dart';
import 'package:gestion_scolaire/ecrans/ecran_enfant.dart';
import 'package:gestion_scolaire/ecrans/onglet_factures.dart';
import 'package:gestion_scolaire/utils/format.dart';
import 'package:gestion_scolaire/widgets/bouton_pdf.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';

http.Response json(int statut, Object? corps) =>
    http.Response.bytes(utf8.encode(jsonEncode(corps)), statut, headers: {'content-type': 'application/json'});

const enfantJson = {
  'eleveId': 7,
  'matricule': 'M-7',
  'nom': 'Diallo',
  'prenom': 'Awa',
  'dateNaissance': '2012-04-05',
  'relation': 'Mère',
  'classeId': 3,
  'classeNom': '6e A',
  'etablissementNom': 'Collège Test',
  'anneeScolaireId': 2,
  'anneeLibelle': '2026-2027',
};

void main() {
  group('formats', () {
    test('notes et écarts', () {
      expect(formaterNote(15.333), '15,33');
      expect(formaterNote(null), '—');
      expect(formaterEcart(1.2), '+1,20');
      expect(formaterEcart(-0.5), '-0,50');
      expect(formaterEcart(null), '—');
    });

    test('rang, date, heure, jour', () {
      expect(formaterRang(1), '1er');
      expect(formaterRang(4), '4e');
      expect(formaterDate('2026-10-26'), '26/10/2026');
      expect(formaterHeure('08:00:00'), '08:00');
      expect(libelleJour(1), 'Lundi');
      expect(libelleJour(7), 'Dimanche');
    });

    test('période par défaut : en cours sinon dernière', () {
      const p1 = Periode(id: 1, libelle: 'T1', dateDebut: '2026-09-01', dateFin: '2026-12-15');
      const p2 = Periode(id: 2, libelle: 'T2', dateDebut: '2027-01-05', dateFin: '2027-03-30');
      expect(periodeParDefaut([p1, p2], DateTime(2026, 10, 28))!.id, 1);
      expect(periodeParDefaut([p1, p2], DateTime(2028, 1, 1))!.id, 2);
      expect(periodeParDefaut([], DateTime(2026, 10, 28)), isNull);
    });
  });

  group('modèles', () {
    test('un enfant sans inscription n\'est pas inscrit', () {
      final e = Enfant.depuisJson({...enfantJson, 'classeId': null, 'anneeScolaireId': null, 'classeNom': null});
      expect(e.estInscrit, isFalse);
      expect(e.nomComplet, 'Awa Diallo');
    });

    test('statut de présence inconnu = présent', () {
      expect(StatutPresence.depuisCode('???'), StatutPresence.present);
      expect(StatutPresence.depuisCode('RETARD'), StatutPresence.retard);
    });

    test('écart d\'une ligne de bulletin', () {
      const l = LigneBulletin(matiere: 'Maths', moyenneEleve: 14, moyenneClasse: 12.5);
      expect(l.ecart, 1.5);
      expect(const LigneBulletin(matiere: 'X', moyenneEleve: 14).ecart, isNull);
    });
  });

  test('le service appelle les bonnes URL', () async {
    final urls = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        urls.add(r.url.toString());
        return json(200, r.url.path.contains('bulletin') ? {'periode': 'T1', 'matieres': [], 'effectif': 0} : []);
      }),
    );
    final service = ServiceScolaire(api);
    await service.mesEnfants();
    await service.periodes(2);
    await service.bulletin(7, 5);
    await service.presences(7, '2026-10-01', '2026-10-28');
    await service.emploiDuTemps(3);

    expect(urls, [
      'http://serveur/api/me/enfants',
      'http://serveur/api/annees-scolaires/2/periodes',
      'http://serveur/api/eleves/7/bulletin?periodeId=5',
      'http://serveur/api/eleves/7/presences?du=2026-10-01&au=2026-10-28',
      'http://serveur/api/classes/3/emploi-du-temps',
    ]);
  });

  testWidgets('écran enfant : bulletin, présences et emploi du temps', (tester) async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        final chemin = r.url.path;
        if (chemin.endsWith('/periodes')) {
          return json(200, [
            {'id': 5, 'anneeScolaireId': 2, 'libelle': 'Trimestre 1', 'ordre': 1, 'dateDebut': '2000-01-01', 'dateFin': '2999-12-31'},
          ]);
        }
        if (chemin.endsWith('/bulletin')) {
          return json(200, {
            'periode': 'Trimestre 1',
            'matieres': [
              {'matiereId': 1, 'matiere': 'Mathématiques', 'moyenneEleve': 14.0, 'moyenneClasse': 12.5},
            ],
            'moyenneGenerale': 14.0,
            'moyenneGeneraleClasse': 12.5,
            'rang': 1,
            'effectif': 30,
          });
        }
        if (chemin.endsWith('/factures')) return json(200, []);
        if (chemin.endsWith('/presences')) {
          return json(200, [
            {'id': 1, 'date': '2026-10-20', 'statut': 'ABSENT', 'motif': 'Malade', 'justifie': true},
            {'id': 2, 'date': '2026-10-21', 'statut': 'PRESENT', 'justifie': false},
          ]);
        }
        return json(200, [
          {
            'matiereLibelle': 'Français',
            'enseignantNom': 'M. Sow',
            'salleNom': 'B2',
            'jourSemaine': 2,
            'heureDebut': '08:00:00',
            'heureFin': '09:00:00',
          },
        ]);
      }),
    );
    await tester.pumpWidget(
      Provider<ServiceScolaire>.value(
        value: ServiceScolaire(api),
        child: MaterialApp(home: EcranEnfant(enfant: Enfant.depuisJson(enfantJson))),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Mathématiques'), findsOneWidget);
    expect(find.text('Écart : +1,50'), findsOneWidget);
    expect(find.text('1er / 30'), findsOneWidget);

    await tester.tap(find.text('Présences'));
    await tester.pumpAndSettle();
    expect(find.text('Absent — 20/10/2026'), findsOneWidget);
    expect(find.text('Absences : 1 (dont 1 justifiée)'), findsOneWidget);

    await tester.tap(find.text('Emploi du temps'));
    await tester.pumpAndSettle();
    expect(find.text('Mardi'), findsOneWidget);
    expect(find.text('Français'), findsOneWidget);
    expect(find.text('08:00\n09:00'), findsOneWidget);
  });

  testWidgets('écran enfant sans inscription', (tester) async {
    final api = ApiClient(urlBase: 'http://serveur', client: MockClient((_) async => json(500, {})));
    await tester.pumpWidget(
      Provider<ServiceScolaire>.value(
        value: ServiceScolaire(api),
        child: MaterialApp(
          home: EcranEnfant(
            enfant: Enfant.depuisJson({...enfantJson, 'classeId': null, 'anneeScolaireId': null, 'classeNom': null}),
          ),
        ),
      ),
    );
    expect(find.textContaining('Aucune inscription active'), findsOneWidget);
  });

  test('formaterMontant suit les décimales de la devise', () {
    expect(formaterMontant(12500, 'XOF'), '12\u202f500\u00a0XOF');
    expect(formaterMontant(12.5, 'EUR'), '12,50\u00a0€');
    expect(formaterMontant(0, 'EUR'), '0,00\u00a0€');
  });

  test('le service des factures choisit la bonne URL', () async {
    final urls = <String>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        urls.add('${r.method} ${r.url.path}');
        return json(200, r.method == 'POST' ? {'reference': 'R1', 'urlPaiement': 'http://x/p/R1'} : []);
      }),
    );
    final service = ServiceScolaire(api);
    await service.factures(Enfant.depuisJson(enfantJson));
    await service.factures(Enfant.depuisJson(enfantJson, estMoi: true));
    final initie = await service.payerEnLigne(9);

    expect(urls, [
      'GET /api/me/enfants/7/factures',
      'GET /api/me/factures',
      'POST /api/me/factures/9/paiements',
    ]);
    expect(initie.urlPaiement, 'http://x/p/R1');
  });

  Map<String, Object?> factureJson(int id, String statut, num reste) => {
        'id': id,
        'numero': 'F-$id',
        'libelle': 'Scolarité $id',
        'montantDu': 1000,
        'montantPaye': 1000 - reste,
        'resteAPayer': reste,
        'devise': 'XOF',
        'echeance': '2026-11-30',
        'statut': statut,
        'enRetard': false,
      };

  testWidgets('factures : seul le reste à payer propose « Payer » et ouvre le prestataire', (tester) async {
    final ouvertes = <Uri>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        if (r.method == 'POST') return json(200, {'reference': 'R1', 'urlPaiement': 'http://pay/R1'});
        return json(200, [factureJson(1, 'IMPAYEE', 1000), factureJson(2, 'PAYEE', 0)]);
      }),
    );
    await tester.pumpWidget(
      Provider<ServiceScolaire>.value(
        value: ServiceScolaire(api),
        child: MaterialApp(
          home: Scaffold(
            body: OngletFactures(
              enfant: Enfant.depuisJson(enfantJson),
              ouvrirUrl: (url) async {
                ouvertes.add(url);
                return true;
              },
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Scolarité 1'), findsOneWidget);
    expect(find.text('Scolarité 2'), findsOneWidget);
    expect(find.text('Payer'), findsOneWidget);

    await tester.tap(find.text('Payer'));
    await tester.pumpAndSettle();
    await tester.tap(find.descendant(of: find.byType(AlertDialog), matching: find.text('Payer')));
    await tester.pumpAndSettle();

    expect(ouvertes, [Uri.parse('http://pay/R1')]);
  });

  testWidgets('factures : une erreur du serveur s\'affiche sans ouvrir de page', (tester) async {
    final ouvertes = <Uri>[];
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        if (r.method == 'POST') return json(409, {'detail': 'Paiement en ligne indisponible'});
        return json(200, [factureJson(1, 'PARTIELLE', 400)]);
      }),
    );
    await tester.pumpWidget(
      Provider<ServiceScolaire>.value(
        value: ServiceScolaire(api),
        child: MaterialApp(
          home: Scaffold(
            body: OngletFactures(
              enfant: Enfant.depuisJson(enfantJson),
              ouvrirUrl: (url) async {
                ouvertes.add(url);
                return true;
              },
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Payer'));
    await tester.pumpAndSettle();
    await tester.tap(find.descendant(of: find.byType(AlertDialog), matching: find.text('Payer')));
    await tester.pumpAndSettle();

    expect(find.text('Paiement en ligne indisponible'), findsOneWidget);
    expect(ouvertes, isEmpty);
  });

  test('le téléchargement envoie le jeton et renvoie les octets', () async {
    String? autorisation;
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((r) async {
        autorisation = r.headers['Authorization'];
        return http.Response.bytes([37, 80, 68, 70], 200);
      }),
    )..definirJeton('jwt');

    final octets = await ServiceScolaire(api).recuPdf(9);

    expect(autorisation, 'Bearer jwt');
    expect(octets, [37, 80, 68, 70]);
  });

  test('une erreur de téléchargement devient une ApiException lisible', () async {
    final api = ApiClient(
      urlBase: 'http://serveur',
      client: MockClient((_) async => json(409, {'detail': 'Aucun paiement validé pour cette facture'})),
    );

    expect(
      () => ServiceScolaire(api).recuPdf(9),
      throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Aucun paiement validé pour cette facture')),
    );
  });

  testWidgets('le bouton PDF télécharge puis enregistre le fichier', (tester) async {
    String? nomEnregistre;
    Uint8List? octetsEnregistres;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: BoutonPdf(
            libelle: 'Reçu (PDF)',
            nomFichier: 'recu.pdf',
            telecharger: () async => Uint8List.fromList([1, 2, 3]),
            enregistrer: (nom, octets) async {
              nomEnregistre = nom;
              octetsEnregistres = octets;
            },
          ),
        ),
      ),
    );

    await tester.tap(find.text('Reçu (PDF)'));
    await tester.pumpAndSettle();

    expect(nomEnregistre, 'recu.pdf');
    expect(octetsEnregistres, [1, 2, 3]);
  });

  testWidgets('le bouton PDF affiche l\'erreur du serveur', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: BoutonPdf(
            libelle: 'Reçu (PDF)',
            nomFichier: 'recu.pdf',
            telecharger: () async => throw ApiException(409, 'Aucun paiement validé pour cette facture'),
            enregistrer: (nom, octets) async {},
          ),
        ),
      ),
    );

    await tester.tap(find.text('Reçu (PDF)'));
    await tester.pumpAndSettle();

    expect(find.text('Aucun paiement validé pour cette facture'), findsOneWidget);
  });
}
