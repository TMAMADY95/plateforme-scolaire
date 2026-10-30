import 'package:flutter/material.dart';

import 'package:provider/provider.dart';

import '../api/modeles_enseignant.dart';
import '../api/service_enseignant.dart';
import 'onglet_annonces.dart';
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
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: Text('Classe ${classe.nom}'),
          bottom: const TabBar(tabs: [Tab(text: 'Appel'), Tab(text: 'Notes'), Tab(text: 'Devoirs'), Tab(text: 'Annonces')]),
        ),
        body: TabBarView(
          children: [
            OngletAppel(classe: classe, maintenant: maintenant),
            OngletNotes(classe: classe),
            OngletDevoirs(
              charger: () => context.read<ServiceEnseignant>().devoirs(classe.classeId),
              messageVide: 'Aucun devoir pour cette classe.',
              maintenant: maintenant,
              matieres: classe.matieresAvecId,
              publier: classe.matieresAvecId.isEmpty
                  ? null
                  : (matiereId, titre, consigne, dateLimite) => context.read<ServiceEnseignant>().publierDevoir(
                        classe.classeId,
                        matiereId: matiereId,
                        titre: titre,
                        consigne: consigne,
                        dateLimite: dateLimite,
                      ),
              supprimer: (d) => context.read<ServiceEnseignant>().supprimerDevoir(d.id),
            ),
            OngletAnnonces(
              charger: () => context.read<ServiceEnseignant>().annonces(classe.classeId),
              messageVide: 'Aucune annonce pour cette classe.',
              publier: (titre, contenu) =>
                  context.read<ServiceEnseignant>().publierAnnonce(classe.classeId, titre: titre, contenu: contenu),
              supprimer: (a) => context.read<ServiceEnseignant>().supprimerAnnonce(a.id),
            ),
          ],
        ),
      ),
    );
  }
}
