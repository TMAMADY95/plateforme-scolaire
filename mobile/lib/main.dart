import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'api/api_client.dart';
import 'app.dart';
import 'auth/etat_authentification.dart';
import 'auth/stockage_jeton.dart';
import 'config.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final api = ApiClient(urlBase: urlApi);
  runApp(
    MultiProvider(
      providers: [
        Provider<ApiClient>.value(value: api),
        ChangeNotifierProvider<EtatAuthentification>(
          create: (_) => EtatAuthentification(api: api, stockage: StockageJetonSecurise())..demarrer(),
        ),
      ],
      child: const Application(),
    ),
  );
}
