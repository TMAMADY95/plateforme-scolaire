import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../api/modeles_enseignant.dart';
import '../api/modeles_scolaires.dart';
import '../api/service_enseignant.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

/// Feuille d'appel : choix de la date, un statut par élève, enregistrement en une fois.
class OngletAppel extends StatefulWidget {
  const OngletAppel({super.key, required this.classe, this.maintenant});

  final ClasseEnseignee classe;
  final DateTime? maintenant;

  @override
  State<OngletAppel> createState() => _EtatOngletAppel();
}

class _EtatOngletAppel extends State<OngletAppel> {
  late DateTime _date = widget.maintenant ?? DateTime.now();
  int _version = 0;

  DateTime get _aujourdhui => widget.maintenant ?? DateTime.now();

  Future<void> _choisirDate() async {
    final choisie = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: _aujourdhui.subtract(const Duration(days: 365)),
      lastDate: _aujourdhui,
      locale: const Locale('fr'),
    );
    if (choisie != null) setState(() => _date = choisie);
  }

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceEnseignant>();
    final jour = dateIso(_date);
    final cle = '$jour-$_version';
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        OutlinedButton.icon(
          onPressed: _choisirDate,
          icon: const Icon(Icons.calendar_today),
          label: Text('Appel du ${formaterDate(jour)}'),
        ),
        const SizedBox(height: 16),
        ChargeurDonnees<FeuilleAppel>(
          cle: cle,
          charger: () => service.feuille(widget.classe.classeId, jour),
          construire: (_, feuille) => _FeuilleEditable(
            key: ValueKey(cle),
            classeId: widget.classe.classeId,
            jour: jour,
            feuille: feuille,
            surEnregistre: () => setState(() => _version++),
          ),
        ),
      ],
    );
  }
}

class _Saisie {
  _Saisie({this.statut, this.motif = '', this.justifie = false});

  StatutPresence? statut;
  String motif;
  bool justifie;
}

class _FeuilleEditable extends StatefulWidget {
  const _FeuilleEditable({
    super.key,
    required this.classeId,
    required this.jour,
    required this.feuille,
    required this.surEnregistre,
  });

  final int classeId;
  final String jour;
  final FeuilleAppel feuille;
  final VoidCallback surEnregistre;

  @override
  State<_FeuilleEditable> createState() => _EtatFeuilleEditable();
}

class _EtatFeuilleEditable extends State<_FeuilleEditable> {
  late final Map<int, _Saisie> _saisies = {
    for (final l in widget.feuille.lignes)
      l.inscriptionId: _Saisie(statut: l.statut, motif: l.motif ?? '', justifie: l.justifie),
  };
  bool _enCours = false;

  int _compte(StatutPresence? statut) => _saisies.values.where((s) => s.statut == statut).length;

  void _toutPresent() {
    setState(() {
      for (final s in _saisies.values) {
        s.statut ??= StatutPresence.present;
      }
    });
  }

  Future<void> _enregistrer() async {
    final lignes = [
      for (final l in widget.feuille.lignes)
        if (_saisies[l.inscriptionId]!.statut != null)
          LigneAppel(
            inscriptionId: l.inscriptionId,
            statut: _saisies[l.inscriptionId]!.statut!,
            motif: _saisies[l.inscriptionId]!.motif,
            justifie: _saisies[l.inscriptionId]!.justifie,
          ),
    ];
    final messager = ScaffoldMessenger.of(context);
    if (lignes.isEmpty) {
      messager.showSnackBar(const SnackBar(content: Text("Indiquez au moins un statut avant d'enregistrer.")));
      return;
    }
    final service = context.read<ServiceEnseignant>();
    final surEnregistre = widget.surEnregistre;
    setState(() => _enCours = true);
    try {
      await service.enregistrerAppel(widget.classeId, widget.jour, lignes);
      messager.showSnackBar(const SnackBar(content: Text('Appel enregistré.')));
      surEnregistre();
    } on ApiException catch (e) {
      messager.showSnackBar(SnackBar(content: Text(e.message)));
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.feuille.lignes.isEmpty) {
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
          '${_compte(StatutPresence.present)} présent(s) · ${_compte(StatutPresence.absent)} absent(s) · '
          '${_compte(StatutPresence.retard)} en retard · ${_compte(null)} non renseigné(s)',
          style: theme.textTheme.bodySmall,
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _compte(null) == 0 ? null : _toutPresent,
                child: const Text('Tout le monde présent'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: FilledButton(
                onPressed: _enCours ? null : _enregistrer,
                child: _enCours
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text("Enregistrer l'appel"),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        for (final ligne in widget.feuille.lignes) _CarteEleve(ligne: ligne, saisie: _saisies[ligne.inscriptionId]!, surChangement: () => setState(() {})),
      ],
    );
  }
}

class _CarteEleve extends StatelessWidget {
  const _CarteEleve({required this.ligne, required this.saisie, required this.surChangement});

  final LigneFeuille ligne;
  final _Saisie saisie;
  final VoidCallback surChangement;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final nonPresent = saisie.statut == StatutPresence.absent || saisie.statut == StatutPresence.retard;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(ligne.nomComplet, style: theme.textTheme.titleSmall),
            Text(ligne.matricule, style: theme.textTheme.bodySmall),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                for (final statut in StatutPresence.values)
                  ChoiceChip(
                    label: Text(statut.libelle),
                    selected: saisie.statut == statut,
                    onSelected: (_) {
                      saisie.statut = statut;
                      surChangement();
                    },
                  ),
              ],
            ),
            if (nonPresent) ...[
              const SizedBox(height: 8),
              TextFormField(
                key: ValueKey('motif-${ligne.inscriptionId}'),
                initialValue: saisie.motif,
                maxLength: 255,
                decoration: const InputDecoration(labelText: 'Motif (facultatif)', counterText: ''),
                onChanged: (v) => saisie.motif = v,
              ),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Justifié'),
                value: saisie.justifie,
                onChanged: (v) {
                  saisie.justifie = v ?? false;
                  surChangement();
                },
              ),
            ],
          ],
        ),
      ),
    );
  }
}
