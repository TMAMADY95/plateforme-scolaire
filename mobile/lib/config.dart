/// Adresse du serveur. Se règle au lancement :
///   flutter run --dart-define=API_URL=http://192.168.1.20:8080
///
/// Par défaut : 10.0.2.2, l'adresse par laquelle l'émulateur Android joint le poste de développement.
/// Simulateur iOS : http://localhost:8080. Téléphone réel : l'adresse IP du poste sur le réseau local.
const String urlApi = String.fromEnvironment('API_URL', defaultValue: 'http://10.0.2.2:8080');
