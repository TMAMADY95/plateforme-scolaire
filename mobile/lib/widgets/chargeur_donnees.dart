import 'package:flutter/material.dart';

import '../api/api_client.dart';

/// Charge des données une fois, affiche l'attente, l'erreur (avec « Réessayer ») ou le résultat.
/// Quand [cle] change, les données sont rechargées.
class ChargeurDonnees<T> extends StatefulWidget {
  const ChargeurDonnees({super.key, required this.charger, required this.construire, this.cle});

  final Future<T> Function() charger;
  final Widget Function(BuildContext contexte, T donnees) construire;
  final Object? cle;

  @override
  State<ChargeurDonnees<T>> createState() => _EtatChargeurDonnees<T>();
}

class _EtatChargeurDonnees<T> extends State<ChargeurDonnees<T>> {
  late Future<T> _futur;

  @override
  void initState() {
    super.initState();
    _futur = widget.charger();
  }

  @override
  void didUpdateWidget(ChargeurDonnees<T> ancien) {
    super.didUpdateWidget(ancien);
    if (ancien.cle != widget.cle) _recharger();
  }

  void _recharger() {
    // Bloc (et non flèche) : la fonction de setState ne doit surtout pas renvoyer le Future.
    setState(() {
      _futur = widget.charger();
    });
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: _futur,
      builder: (contexte, instantane) {
        if (instantane.connectionState != ConnectionState.done) {
          return const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()));
        }
        if (instantane.hasError) {
          final erreur = instantane.error;
          return Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  erreur is ApiException ? erreur.message : 'Une erreur est survenue.',
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 12),
                FilledButton.tonal(onPressed: _recharger, child: const Text('Réessayer')),
              ],
            ),
          );
        }
        return widget.construire(contexte, instantane.requireData);
      },
    );
  }
}
