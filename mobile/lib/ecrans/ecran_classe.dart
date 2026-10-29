import 'package:flutter/material.dart';

import 'package:provider/provider.dart';

import '../api/modeles_enseignant.dart';
import '../api/service_enseignant.dart';
import 'onglet_appel.dart';
import 'onglet_devoirs.dart';
import 'onglet_notes.dart';

/// Une classe de l'enseignant : l'appel et la saisie des notes.
class EcranClasse extends StatelessWidget {
  const EcranClasse({super.key, required this.classe, this.maintenant});

  final ClasseEnseignee classe;

  /// Date du jour, injectable pour les tests.
  final DateTime? maintenant;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: Text('Classe ${classe.nom}'),
          bottom: const TabBar(tabs: [Tab(text: 'Appel'), Tab(text: 'Notes'), Tab(text: 'Devoirs')]),
        ),
        body: TabBarView(
          children: [
            OngletAppel(classe: classe, maintenant: maintenant),
            OngletNotes(classe: classe),
            OngletDevoirs(
              charger: () => context.read<ServiceEnseignant>().devoirs(classe.classeId),
              messageVide: 'Aucun devoir pour cette classe. Publiez-en depuis l\'application web.',
            ),
          ],
        ),
      ),
    );
  }
}
