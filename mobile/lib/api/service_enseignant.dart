import 'api_client.dart';
import 'modeles_enseignant.dart';
import 'modeles_scolaires.dart';

/// Les appels de l'espace enseignant.
class ServiceEnseignant {
  ServiceEnseignant(this._api);

  final ApiClient _api;

  Future<List<ClasseEnseignee>> mesClasses() async {
    final json = await _api.get('/api/me/affectations') as List<dynamic>? ?? const [];
    return ClasseEnseignee.grouper(
      json.whereType<Map<String, dynamic>>().map(AffectationEnseignant.depuisJson).toList(),
    );
  }

  /// [date] au format AAAA-MM-JJ.
  Future<FeuilleAppel> feuille(int classeId, String date) async =>
      FeuilleAppel.depuisJson(await _api.get('/api/classes/$classeId/presences?date=$date') as Map<String, dynamic>);

  Future<FeuilleAppel> enregistrerAppel(int classeId, String date, List<LigneAppel> lignes) async =>
      FeuilleAppel.depuisJson(
        await _api.put(
          '/api/classes/$classeId/presences/$date',
          corps: {'lignes': lignes.map((l) => l.versJson()).toList()},
        ) as Map<String, dynamic>,
      );

  Future<List<EvaluationClasse>> evaluations(int classeId) async {
    final json = await _api.get('/api/classes/$classeId/evaluations') as List<dynamic>? ?? const [];
    return json.whereType<Map<String, dynamic>>().map(EvaluationClasse.depuisJson).toList();
  }

  Future<List<Devoir>> devoirs(int classeId) async =>
      Devoir.listeDepuisJson(await _api.get('/api/classes/$classeId/devoirs'));

  Future<GrilleNotes> grille(int evaluationId) async =>
      GrilleNotes.depuisJson(await _api.get('/api/evaluations/$evaluationId') as Map<String, dynamic>);

  Future<GrilleNotes> enregistrerNotes(int evaluationId, List<SaisieNote> lignes) async => GrilleNotes.depuisJson(
        await _api.put(
          '/api/evaluations/$evaluationId/notes',
          corps: {'lignes': lignes.map((l) => l.versJson()).toList()},
        ) as Map<String, dynamic>,
      );
}
