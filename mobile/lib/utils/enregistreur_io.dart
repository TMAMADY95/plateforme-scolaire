import 'dart:io';
import 'dart:typed_data';

import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Écrit le fichier dans le dossier temporaire puis l'ouvre. Lève une [Exception] lisible si rien ne peut l'ouvrir.
Future<void> enregistrerEtOuvrir(String nom, Uint8List octets) async {
  final dossier = await getTemporaryDirectory();
  final fichier = File('${dossier.path}${Platform.pathSeparator}$nom');
  await fichier.writeAsBytes(octets, flush: true);
  final resultat = await OpenFilex.open(fichier.path);
  if (resultat.type != ResultType.done) {
    throw Exception("Aucune application n'a pu ouvrir le fichier.");
  }
}
