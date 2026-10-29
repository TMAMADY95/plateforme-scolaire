import 'dart:typed_data';

import 'api_client.dart';
import 'modeles_scolaires.dart';

/// Les appels de lecture réservés aux parents et aux élèves.
class ServiceScolaire {
  ServiceScolaire(this._api);

  final ApiClient _api;

  Future<List<Enfant>> mesEnfants() async => enfantsDepuisJson(await _api.get('/api/me/enfants'));

  /// Le dossier de l'élève connecté (réservé au rôle ELEVE).
  Future<Enfant> monProfil() async => Enfant.depuisJson(await _api.get('/api/me/eleve') as Map<String, dynamic>, estMoi: true);

  Future<List<Periode>> periodes(int anneeScolaireId) async =>
      Periode.listeDepuisJson(await _api.get('/api/annees-scolaires/$anneeScolaireId/periodes'));

  Future<Bulletin> bulletin(int eleveId, int periodeId) async =>
      Bulletin.depuisJson(await _api.get('/api/eleves/$eleveId/bulletin?periodeId=$periodeId') as Map<String, dynamic>);

  /// [du] et [au] au format AAAA-MM-JJ.
  Future<List<PresenceEleve>> presences(int eleveId, String du, String au) async =>
      PresenceEleve.listeDepuisJson(await _api.get('/api/eleves/$eleveId/presences?du=$du&au=$au'));

  Future<List<CreneauEleve>> emploiDuTemps(int classeId) async =>
      CreneauEleve.listeDepuisJson(await _api.get('/api/classes/$classeId/emploi-du-temps'));

  /// Devoirs de la classe, par date limite croissante.
  Future<List<Devoir>> devoirs(int classeId) async =>
      Devoir.listeDepuisJson(await _api.get('/api/classes/$classeId/devoirs'));

  /// Factures de l'élève connecté, ou d'un enfant du parent connecté.
  Future<List<Facture>> factures(Enfant enfant) async => Facture.listeDepuisJson(
        await _api.get(enfant.estMoi ? '/api/me/factures' : '/api/me/enfants/${enfant.eleveId}/factures'),
      );

  /// Démarre un paiement en ligne du reste à payer ; le paiement se termine chez le prestataire.
  Future<PaiementInitie> payerEnLigne(int factureId) async =>
      PaiementInitie.depuisJson(await _api.post('/api/me/factures/$factureId/paiements', corps: {}) as Map<String, dynamic>);

  Future<Uint8List> bulletinPdf(int eleveId, int periodeId) =>
      _api.telecharger('/api/eleves/$eleveId/bulletin.pdf?periodeId=$periodeId');

  Future<Uint8List> recuPdf(int factureId) => _api.telecharger('/api/factures/$factureId/recu.pdf');
}
