import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/modeles_enseignant.dart';
import '../api/modeles_scolaires.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';
import '../widgets/formulaires_publication.dart';

/// Devoirs d'une classe. Avec [surBascule] (élève, parent), chaque devoir a une case « fait » ;
/// sans (enseignant), on voit combien d'élèves ont terminé.
class OngletDevoirs extends StatefulWidget {
  const OngletDevoirs({
    super.key,
    required this.charger,
    this.surBascule,
    this.messageVide = 'Aucun devoir à venir.',
    this.maintenant,
    this.matieres = const [],
    this.publier,
    this.supprimer,
  });

  final Future<List<Devoir>> Function() charger;
  final Future<void> Function(Devoir devoir, bool fait)? surBascule;
  final String messageVide;

  /// Date du jour, injectable pour les tests.
  final DateTime? maintenant;

  /// Enseignant : matières qu'il assure dans la classe. Avec [publier], un bouton permet de créer un devoir ;
  /// avec [supprimer], il peut retirer ceux de ses matières.
  final List<MatiereEnseignee> matieres;
  final Future<void> Function(int matiereId, String titre, String consigne, String dateLimite)? publier;
  final Future<void> Function(Devoir devoir)? supprimer;

  @override
  State<OngletDevoirs> createState() => _EtatOngletDevoirs();
}

class _EtatOngletDevoirs extends State<OngletDevoirs> with AutomaticKeepAliveClientMixin {
  /// Cases cochées ou décochées depuis le chargement : la case réagit tout de suite.
  final Map<int, bool> _choix = {};
  int _version = 0;

  @override
  bool get wantKeepAlive => true;

  bool _fait(Devoir d) => _choix[d.id] ?? d.fait ?? false;

  Future<void> _basculer(Devoir d, bool fait) async {
    final ancien = _fait(d);
    setState(() => _choix[d.id] = fait);
    try {
      await widget.surBascule!(d, fait);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _choix[d.id] = ancien);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  void _message(String texte) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texte)));

  Future<void> _executer(Future<void> Function() action, String succes) async {
    try {
      await action();
      if (!mounted) return;
      setState(() => _version++);
      _message(succes);
    } on ApiException catch (e) {
      if (mounted) _message(e.message);
    }
  }

  Future<void> _creer() async {
    final saisie = await demanderDevoir(context,
        matieres: widget.matieres, aujourdhui: widget.maintenant ?? DateTime.now());
    if (saisie == null || !mounted) return;
    await _executer(
        () => widget.publier!(saisie.matiereId, saisie.titre, saisie.consigne, saisie.dateLimite), 'Devoir publié.');
  }

  Future<void> _retirer(Devoir d) async {
    if (!await confirmer(context, 'Supprimer le devoir « ${d.titre} » ?') || !mounted) return;
    await _executer(() => widget.supprimer!(d), 'Devoir supprimé.');
  }

  bool _supprimable(Devoir d) =>
      widget.supprimer != null && d.matiereId != null && widget.matieres.any((m) => m.id == d.matiereId);

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final aujourdhui = dateIso(widget.maintenant ?? DateTime.now());
    final liste = ChargeurDonnees<List<Devoir>>(
      charger: widget.charger,
      cle: _version,
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
                  padding: const EdgeInsets.fromLTRB(8, 12, 16, 12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (widget.surBascule != null)
                        Checkbox(value: _fait(d), onChanged: (v) => _basculer(d, v ?? false))
                      else
                        const SizedBox(width: 8),
                      Expanded(
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
                                  _fait(d) ? 'Fait' : 'Pour le ${formaterDate(d.dateLimite)}',
                                  style: theme.textTheme.labelLarge?.copyWith(
                                    color: _fait(d)
                                        ? Colors.green.shade700
                                        : d.dateLimite.compareTo(aujourdhui) < 0
                                            ? theme.colorScheme.outline
                                            : d.dateLimite == aujourdhui
                                                ? Colors.orange.shade800
                                                : theme.colorScheme.primary,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    d.titre,
                                    style: theme.textTheme.titleMedium?.copyWith(
                                      decoration: _fait(d) ? TextDecoration.lineThrough : null,
                                    ),
                                  ),
                                ),
                                if (_supprimable(d))
                                  IconButton(
                                    tooltip: 'Supprimer le devoir',
                                    icon: const Icon(Icons.delete_outline),
                                    onPressed: () => _retirer(d),
                                  ),
                              ],
                            ),
                            if (d.consigne != null) ...[const SizedBox(height: 6), Text(d.consigne!)],
                            if (d.faits != null && d.effectif != null) ...[
                              const SizedBox(height: 6),
                              Text('${d.faits}/${d.effectif} élèves ont terminé', style: theme.textTheme.bodySmall),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
    if (widget.publier == null) return liste;
    return Column(
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: FilledButton.icon(onPressed: _creer, icon: const Icon(Icons.add), label: const Text('Nouveau devoir')),
          ),
        ),
        Expanded(child: liste),
      ],
    );
  }
}
