import 'package:flutter/material.dart';

import 'package:provider/provider.dart';

import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import 'onglet_bulletin.dart';
import 'onglet_devoirs.dart';
import 'onglet_emploi_du_temps.dart';
import 'onglet_factures.dart';
import 'onglet_presences.dart';

/// Le détail d'un enfant (ou de l'élève connecté) : bulletin, présences, emploi du temps.
class EcranEnfant extends StatelessWidget {
  const EcranEnfant({super.key, required this.enfant});

  final Enfant enfant;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 5,
      child: Scaffold(
        appBar: AppBar(
          title: Text(enfant.nomComplet),
          bottom: const TabBar(
            isScrollable: true,
            tabAlignment: TabAlignment.start,
            tabs: [
              Tab(text: 'Bulletin'),
              Tab(text: 'Devoirs'),
              Tab(text: 'Présences'),               Tab(text: 'Emploi du temps'),
              Tab(text: 'Factures'),
            ],
          ),
        ),
        body: enfant.estInscrit
            ? TabBarView(
                children: [
                  OngletBulletin(enfant: enfant),
                  OngletDevoirs(charger: () => context.read<ServiceScolaire>().devoirs(enfant.classeId!)),
                  OngletPresences(enfant: enfant),
                  OngletEmploiDuTemps(enfant: enfant),
                  OngletFactures(enfant: enfant),
                ],
              )
            : Padding(
                padding: const EdgeInsets.all(24),
                child: Center(
                  child: Text(
                    "Aucune inscription active : il n'y a pas encore de bulletin, de présences ni d'emploi du temps.",
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                ),
              ),
      ),
    );
  }
}
