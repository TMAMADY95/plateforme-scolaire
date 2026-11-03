import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

/// Résultats proclamés des examens nationaux (CEE, BEPC, baccalauréat) d'un enfant ou de l'élève connecté.
class OngletExamens extends StatefulWidget {
  const OngletExamens({super.key, required this.enfant});

  final Enfant enfant;

  @override
  State<OngletExamens> createState() => _EtatOngletExamens();
}

class _EtatOngletExamens extends State<OngletExamens> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return ChargeurDonnees<List<ResultatExamen>>(
      charger: () => context.read<ServiceScolaire>().resultatsExamens(widget.enfant),
      construire: (contexte, resultats) => ListeResultatsExamens(resultats: resultats),
    );
  }
}

/// Affichage des résultats (séparé du chargement pour pouvoir être testé seul).
class ListeResultatsExamens extends StatelessWidget {
  const ListeResultatsExamens({super.key, required this.resultats});

  final List<ResultatExamen> resultats;

  @override
  Widget build(BuildContext context) {
    if (resultats.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: Text("Aucun résultat d'examen national proclamé pour le moment.", textAlign: TextAlign.center)),
      );
    }
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [for (final r in resultats) _CarteResultat(resultat: r)],
    );
  }
}

class _CarteResultat extends StatelessWidget {
  const _CarteResultat({required this.resultat});

  final ResultatExamen resultat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final admis = resultat.decision == 'ADMIS';
    final sousTitre = [
      if (resultat.numero != null) 'N° ${resultat.numero}',
      if (resultat.dateProclamation != null) 'Proclamé le ${formaterDate(resultat.dateProclamation!)}',
      if (resultat.moyenne != null) 'Moyenne ${formaterNote(resultat.moyenne)}/${resultat.bareme}',
    ].join(' · ');
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${resultat.examen} (${resultat.anneeScolaire})', style: theme.textTheme.titleMedium),
            const SizedBox(height: 4),
            Text(
              resultat.libelleResultat,
              style: theme.textTheme.titleSmall?.copyWith(color: admis ? Colors.green.shade700 : theme.colorScheme.error),
            ),
            if (sousTitre.isNotEmpty) Text(sousTitre, style: theme.textTheme.bodySmall),
            if (resultat.notes.isNotEmpty) ...[
              const Divider(),
              for (final n in resultat.notes)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Row(
                    children: [
                      Expanded(child: Text(n.epreuve)),
                      Text('coef. ${n.coefficient}  '),
                      Text(formaterNote(n.note), style: const TextStyle(fontWeight: FontWeight.w600)),
                    ],
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}
