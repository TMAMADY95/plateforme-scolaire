import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Où le jeton de connexion est conservé entre deux lancements de l'application.
abstract class StockageJeton {
  Future<String?> lire();
  Future<void> ecrire(String jeton);
  Future<void> effacer();
}

/// Stockage chiffré du système (Keychain sur iOS, Keystore sur Android).
class StockageJetonSecurise implements StockageJeton {
  StockageJetonSecurise([FlutterSecureStorage? stockage]) : _stockage = stockage ?? const FlutterSecureStorage();

  static const _cle = 'jeton_connexion';
  final FlutterSecureStorage _stockage;

  @override
  Future<String?> lire() => _stockage.read(key: _cle);

  @override
  Future<void> ecrire(String jeton) => _stockage.write(key: _cle, value: jeton);

  @override
  Future<void> effacer() => _stockage.delete(key: _cle);
}

/// Stockage en mémoire, pour les tests.
class StockageJetonMemoire implements StockageJeton {
  StockageJetonMemoire([this.jeton]);

  String? jeton;

  @override
  Future<String?> lire() async => jeton;

  @override
  Future<void> ecrire(String valeur) async => jeton = valeur;

  @override
  Future<void> effacer() async => jeton = null;
}
