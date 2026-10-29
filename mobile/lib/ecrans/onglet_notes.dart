import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../api/modeles_enseignant.dart';
import '../api/service_enseignant.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

/// Liste des évaluations de la classe ; un appui ouvre la grille de saisie.
class OngletNotes extends StatefulWidget {
  const OngletNotes({super.key, required this.classe});

  final ClasseEnseignee classe;

  @override
  State<OngletNotes> createState() => _EtatOngletNotes();
}

class _EtatOngletNotes extends State<OngletNotes> {
  int _version = 0;

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceEnseignant>();
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ChargeurDonnees<List<EvaluationClasse>>(
          cle: _version,
          charger: () => service.evaluations(widget.classe.classeId),
          construire: (contexte, evaluations) {
            if (evaluations.isEmpty) {
              return const Padding(
                padding: EdgeInsets.all(16),
                child: Text("Aucune évaluation pour cette classe. Créez-en une depuis l'application web."),
              );
            }
            return Column(
              children: [
                for (final e in evaluations)
                  Card(
                    child: ListTile(
                      title: Text(e.titre),
                      subtitle: Text('${e.matiere} · ${e.periode} · ${formaterDate(e.date)}'),
                      trailing: Text('/${e.bareme.toStringAsFixed(0)}'),
                      onTap: () async {
                        await Navigator.of(contexte).push(
                          MaterialPageRoute<void>(builder: (_) => EcranGrilleNotes(evaluation: e)),
                        );
                        if (mounted) setState(() => _version++);
                      },
                    ),
                  ),
              ],
            );
          },
        ),
      ],
    );
  }
}

class EcranGrilleNotes extends StatelessWidget {
  const EcranGrilleNotes({super.key, required this.evaluation});

  final EvaluationClasse evaluation;

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceEnseignant>();
    return Scaffold(
      appBar: AppBar(title: Text(evaluation.titre)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ChargeurDonnees<GrilleNotes>(
            charger: () => service.grille(evaluation.id),
            construire: (_, grille) => _GrilleEditable(grille: grille),
          ),
        ],
      ),
    );
  }
}

class _GrilleEditable extends StatefulWidget {
  const _GrilleEditable({required this.grille});

  final GrilleNotes grille;

  @override
  State<_GrilleEditable> createState() => _EtatGrilleEditable();
}

class _EtatGrilleEditable extends State<_GrilleEditable> {
  late final Map<int, TextEditingController> _controleurs = {
    for (final l in widget.grille.lignes)
      l.inscriptionId: TextEditingController(
        text: l.valeur == null ? '' : formaterNote(l.valeur),
      ),
  };
  late final Map<int, bool> _absents = {for (final l in widget.grille.lignes) l.inscriptionId: l.absent};
  bool _enCours = false;

  double get _bareme => widget.grille.evaluation.bareme;

  @override
  void dispose() {
    for (final c in _controleurs.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _enregistrer() async {
    final messager = ScaffoldMessenger.of(context);
    final saisies = <SaisieNote>[];
    for (final l in widget.grille.lignes) {
      final absent = _absents[l.inscriptionId] ?? false;
      try {
        final valeur = absent ? null : lireNote(_controleurs[l.inscriptionId]!.text, _bareme);
        saisies.add(SaisieNote(inscriptionId: l.inscriptionId, valeur: valeur, absent: absent));
      } on FormatException catch (e) {
        messager.showSnackBar(SnackBar(content: Text('${l.nomComplet} : ${e.message}')));
        return;
      }
    }
    final service = context.read<ServiceEnseignant>();
    setState(() => _enCours = true);
    try {
      await service.enregistrerNotes(widget.grille.evaluation.id, saisies);
      messager.showSnackBar(const SnackBar(content: Text('Notes enregistrées.')));
    } on ApiException catch (e) {
      messager.showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final grille = widget.grille;
    if (grille.lignes.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(16),
        child: Text("Aucun élève n'est encore inscrit dans cette classe."),
      );
    }
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          '${grille.evaluation.matiere} · ${grille.evaluation.periode} · '
          'coefficient ${grille.evaluation.coefficient.toString().replaceAll('.', ',')} · sur ${_bareme.toStringAsFixed(0)}',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: 8),
        FilledButton(
          onPressed: _enCours ? null : _enregistrer,
          child: _enCours
              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Enregistrer les notes'),
        ),
        const SizedBox(height: 8),
        for (final l in grille.lignes)
          Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(l.nomComplet, style: theme.textTheme.titleSmall),
                        Text(l.matricule, style: theme.textTheme.bodySmall),
                        Row(
                          children: [
                            Checkbox(
                              key: ValueKey('absent-${l.inscriptionId}'),
                              value: _absents[l.inscriptionId],
                              onChanged: (v) => setState(() => _absents[l.inscriptionId] = v ?? false),
                            ),
                            const Text('Absent'),
                          ],
                        ),
                      ],
                    ),
                  ),
                  SizedBox(
                    width: 90,
                    child: TextField(
                      key: ValueKey('note-${l.inscriptionId}'),
                      controller: _controleurs[l.inscriptionId],
                      enabled: !(_absents[l.inscriptionId] ?? false),
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      textAlign: TextAlign.end,
                      decoration: InputDecoration(labelText: 'Note', suffixText: '/${_bareme.toStringAsFixed(0)}'),
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
