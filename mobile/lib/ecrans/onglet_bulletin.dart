import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import '../utils/format.dart';
import '../widgets/bouton_pdf.dart';
import '../widgets/chargeur_donnees.dart';

class OngletBulletin extends StatefulWidget {
  const OngletBulletin({super.key, required this.enfant, this.maintenant});

  final Enfant enfant;

  /// Date du jour, injectable pour les tests.
  final DateTime? maintenant;

  @override
  State<OngletBulletin> createState() => _EtatOngletBulletin();
}

class _EtatOngletBulletin extends State<OngletBulletin> with AutomaticKeepAliveClientMixin {
  int? _periodeId;

  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final service = context.read<ServiceScolaire>();
    return ChargeurDonnees<List<Periode>>(
      charger: () => service.periodes(widget.enfant.anneeScolaireId!),
      construire: (contexte, periodes) {
        if (periodes.isEmpty) {
          return const Center(child: Padding(padding: EdgeInsets.all(24), child: Text('Aucune période définie.')));
        }
        final choisie = periodes.firstWhere(
          (p) => p.id == _periodeId,
          orElse: () => periodeParDefaut(periodes, widget.maintenant ?? DateTime.now())!,
        );
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            InputDecorator(
              decoration: const InputDecoration(labelText: 'Période'),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  isExpanded: true,
                  isDense: true,
                  value: choisie.id,
                  items: [for (final p in periodes) DropdownMenuItem(value: p.id, child: Text(p.libelle))],
                  onChanged: (id) => setState(() => _periodeId = id),
                ),
              ),
            ),
            Align(
              alignment: Alignment.centerRight,
              child: BoutonPdf(
                libelle: 'Télécharger en PDF',
                nomFichier: 'bulletin-${widget.enfant.eleveId}-${choisie.id}.pdf',
                telecharger: () => service.bulletinPdf(widget.enfant.eleveId, choisie.id),
              ),
            ),
            ChargeurDonnees<Bulletin>(
              cle: choisie.id,
              charger: () => service.bulletin(widget.enfant.eleveId, choisie.id),
              construire: (_, bulletin) => _ContenuBulletin(bulletin: bulletin),
            ),
          ],
        );
      },
    );
  }
}

class _ContenuBulletin extends StatelessWidget {
  const _ContenuBulletin({required this.bulletin});

  final Bulletin bulletin;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(child: _Chiffre(titre: 'Moyenne générale', valeur: formaterNote(bulletin.moyenneGenerale))),
                Expanded(
                  child: _Chiffre(titre: 'Moyenne de la classe', valeur: formaterNote(bulletin.moyenneGeneraleClasse)),
                ),
                Expanded(
                  child: _Chiffre(
                    titre: 'Rang',
                    valeur: bulletin.rang == null ? '—' : '${formaterRang(bulletin.rang!)} / ${bulletin.effectif}',
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        if (bulletin.matieres.isEmpty)
          const Padding(padding: EdgeInsets.all(16), child: Text('Aucune note pour cette période.'))
        else
          for (final ligne in bulletin.matieres)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(ligne.matiere),
              subtitle: Text('Coef. ${formaterCoefficient(ligne.coefficient)} · Classe : ${formaterNote(ligne.moyenneClasse)}'),
              trailing: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(formaterNote(ligne.moyenneEleve), style: theme.textTheme.titleMedium),
                  Text(
                    'Écart : ${formaterEcart(ligne.ecart)}',
                    style: theme.textTheme.bodySmall?.copyWith(color: _couleurEcart(theme, ligne.ecart)),
                  ),
                ],
              ),
            ),
      ],
    );
  }

  Color? _couleurEcart(ThemeData theme, double? ecart) {
    if (ecart == null || ecart == 0) return null;
    return ecart > 0 ? Colors.green.shade700 : theme.colorScheme.error;
  }
}

class _Chiffre extends StatelessWidget {
  const _Chiffre({required this.titre, required this.valeur});

  final String titre;
  final String valeur;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        Text(valeur, style: theme.textTheme.titleLarge),
        const SizedBox(height: 4),
        Text(titre, style: theme.textTheme.bodySmall, textAlign: TextAlign.center),
      ],
    );
  }
}
