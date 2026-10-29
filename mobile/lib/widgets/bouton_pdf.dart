import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../utils/enregistreur.dart';

/// Bouton qui télécharge un PDF puis l'ouvre ; une erreur s'affiche en bas de l'écran.
class BoutonPdf extends StatefulWidget {
  const BoutonPdf({
    super.key,
    required this.libelle,
    required this.nomFichier,
    required this.telecharger,
    this.enregistrer = enregistrerEtOuvrir,
  });

  final String libelle;
  final String nomFichier;
  final Future<Uint8List> Function() telecharger;

  /// Enregistre et ouvre le fichier (remplaçable dans les tests).
  final Future<void> Function(String nom, Uint8List octets) enregistrer;

  @override
  State<BoutonPdf> createState() => _EtatBoutonPdf();
}

class _EtatBoutonPdf extends State<BoutonPdf> {
  bool _enCours = false;

  Future<void> _lancer() async {
    final messager = ScaffoldMessenger.of(context);
    setState(() => _enCours = true);
    try {
      final octets = await widget.telecharger();
      await widget.enregistrer(widget.nomFichier, octets);
    } on ApiException catch (e) {
      messager.showSnackBar(SnackBar(content: Text(e.message)));
    } catch (e) {
      messager.showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
      onPressed: _enCours ? null : _lancer,
      icon: _enCours
          ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
          : const Icon(Icons.picture_as_pdf),
      label: Text(widget.libelle),
    );
  }
}
