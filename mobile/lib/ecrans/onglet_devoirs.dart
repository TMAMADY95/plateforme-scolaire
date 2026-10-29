import 'package:flutter/material.dart';

import '../api/modeles_scolaires.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

/// Devoirs d'une classe, en lecture seule (élève, parent et enseignant).
class OngletDevoirs extends StatefulWidget {
  const OngletDevoirs({super.key, required this.charger, this.messageVide = 'Aucun devoir à venir.', this.maintenant});

  final Future<List<Devoir>> Function() charger;
  final String messageVide;

  /// Date du jour, injectable pour les tests.
  final DateTime? maintenant;

  @override
  State<OngletDevoirs> createState() => _EtatOngletDevoirs();
}

class _EtatOngletDevoirs extends State<OngletDevoirs> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final aujourdhui = dateIso(widget.maintenant ?? DateTime.now());
    return ChargeurDonnees<List<Devoir>>(
      charger: widget.charger,
      construire: (contexte, devoirs) {
        if (devoirs.isEmpty) {
          return Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(widget.messageVide)));
        }
        final theme = Theme.of(contexte);
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            for (final d in devoirs)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              [d.matiere, d.enseignant].where((t) => t.isNotEmpty).join(' · '),
                              style: theme.textTheme.bodySmall,
                            ),
                          ),
                          Text(
                            'Pour le ${formaterDate(d.dateLimite)}',
                            style: theme.textTheme.labelLarge?.copyWith(
                              color: d.dateLimite.compareTo(aujourdhui) < 0
                                  ? theme.colorScheme.outline
                                  : d.dateLimite == aujourdhui
                                      ? Colors.orange.shade800
                                      : theme.colorScheme.primary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(d.titre, style: theme.textTheme.titleMedium),
                      if (d.consigne != null) ...[const SizedBox(height: 6), Text(d.consigne!)],
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}
