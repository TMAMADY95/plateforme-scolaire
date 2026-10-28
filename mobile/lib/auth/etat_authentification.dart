import 'package:flutter/foundation.dart';

import '../api/api_client.dart';
import '../api/modeles.dart';
import 'stockage_jeton.dart';

enum Phase {
  /// Lecture du jeton enregistré et vérification auprès du serveur.
  initialisation,
  deconnecte,
  connecte,

  /// Le serveur n'a pas pu être joint au démarrage : on propose de réessayer plutôt que de déconnecter.
  horsLigne,
}

/// Session de l'utilisateur : connexion, déconnexion, changement de mot de passe.
class EtatAuthentification extends ChangeNotifier {
  EtatAuthentification({required this.api, required StockageJeton stockage}) : _stockage = stockage {
    api.surNonAutorise = () => _terminerSession('Votre session a expiré, veuillez vous reconnecter.');
    api.surMotDePasseRequis = () {
      final actuel = _moi;
      if (actuel != null && !actuel.changementMotDePasseRequis) {
        _moi = Moi(
          id: actuel.id,
          email: actuel.email,
          changementMotDePasseRequis: true,
          affectations: actuel.affectations,
        );
        notifyListeners();
      }
    };
  }

  final ApiClient api;
  final StockageJeton _stockage;

  Phase _phase = Phase.initialisation;
  Moi? _moi;
  String? _message;

  Phase get phase => _phase;
  Moi? get moi => _moi;

  /// Information à afficher sur l'écran de connexion (session expirée, mot de passe changé...).
  String? get message => _message;

  /// Au lancement : reprend la session enregistrée si le serveur l'accepte encore.
  Future<void> demarrer() async {
    final jeton = await _stockage.lire();
    if (jeton == null) {
      _phase = Phase.deconnecte;
      notifyListeners();
      return;
    }
    api.definirJeton(jeton);
    try {
      _moi = await _chargerMoi();
      _phase = Phase.connecte;
    } on ApiException catch (e) {
      if (e.statut == 0) {
        _phase = Phase.horsLigne;
      } else {
        await _effacerSession();
        _phase = Phase.deconnecte;
      }
    }
    notifyListeners();
  }

  /// Lève [ApiException] avec un message lisible si la connexion échoue.
  Future<void> connexion(String email, String motDePasse) async {
    final json = await api.post(
      '/api/auth/login',
      corps: {'email': email.trim(), 'motDePasse': motDePasse},
      sansJeton: true,
    );
    final reponse = ReponseConnexion.depuisJson(json as Map<String, dynamic>);
    api.definirJeton(reponse.jeton);
    try {
      _moi = await _chargerMoi();
    } catch (_) {
      api.definirJeton(null);
      rethrow;
    }
    await _stockage.ecrire(reponse.jeton);
    _message = null;
    _phase = Phase.connecte;
    notifyListeners();
  }

  /// Après un changement réussi, le serveur révoque les anciens jetons : on repasse par la connexion.
  Future<void> changerMotDePasse(String actuel, String nouveau) async {
    await api.post('/api/me/mot-de-passe', corps: {'motDePasseActuel': actuel, 'nouveauMotDePasse': nouveau});
    await _terminerSession('Mot de passe modifié. Connectez-vous avec le nouveau.');
  }

  Future<void> deconnexion() => _terminerSession(null);

  Future<Moi> _chargerMoi() async {
    final json = await api.get('/api/me');
    return Moi.depuisJson(json as Map<String, dynamic>);
  }

  Future<void> _terminerSession(String? message) async {
    await _effacerSession();
    _message = message;
    _phase = Phase.deconnecte;
    notifyListeners();
  }

  Future<void> _effacerSession() async {
    api.definirJeton(null);
    _moi = null;
    await _stockage.effacer();
  }
}
