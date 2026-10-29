import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

/// Déclenche le téléchargement dans le navigateur.
Future<void> enregistrerEtOuvrir(String nom, Uint8List octets) async {
  final blob = web.Blob([octets.toJS].toJS, web.BlobPropertyBag(type: 'application/pdf'));
  final url = web.URL.createObjectURL(blob);
  final lien = web.HTMLAnchorElement()
    ..href = url
    ..download = nom;
  lien.click();
  web.URL.revokeObjectURL(url);
}
