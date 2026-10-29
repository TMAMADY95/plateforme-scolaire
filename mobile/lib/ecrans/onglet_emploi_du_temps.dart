import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

class OngletEmploiDuTemps extends StatefulWidget {
  const OngletEmploiDuTemps({super.key, required this.enfant});

  final Enfant enfant;

  @override
  State<OngletEmploiDuTemps> createState() => _EtatOngletEmploiDuTemps();
}

class _EtatOngletEmploiDuTemps extends State<OngletEmploiDuTemps> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final service = context.read<ServiceScolaire>();
    return ChargeurDonnees<List<CreneauEleve>>(
      charger: () => service.emploiDuTemps(widget.enfant.classeId!),
      construire: (contexte, creneaux) {
        if (creneaux.isEmpty) {
          return const Center(
            child: Padding(padding: EdgeInsets.all(24), child: Text("Aucun cours n'est encore planifié.")),
          );
        }
        final parJour = <int, List<CreneauEleve>>{};
        for (final c in creneaux) {
          parJour.putIfAbsent(c.jourSemaine, () => []).add(c);
        }
        final jours = parJour.keys.toList()..sort();
        final theme = Theme.of(contexte);
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            for (final jour in jours) ...[
              Padding(
                padding: const EdgeInsets.only(top: 8, bottom: 4),
                child: Text(libelleJour(jour), style: theme.textTheme.titleMedium),
              ),
              for (final c in (parJour[jour]!..sort((a, b) => a.heureDebut.compareTo(b.heureDebut))))
                Card(
                  child: ListTile(
                    title: Text(c.matiere),
                    subtitle: Text([
                      if (c.enseignantNom != null) c.enseignantNom!,
                      if (c.salleNom != null) 'Salle ${c.salleNom}',
                    ].join(' · ')),
                    trailing: Text('${formaterHeure(c.heureDebut)}\n${formaterHeure(c.heureFin)}',
                        textAlign: TextAlign.end),
                  ),
                ),
            ],
          ],
        );
      },
    );
  }
}
