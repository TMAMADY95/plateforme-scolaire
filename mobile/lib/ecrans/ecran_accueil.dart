import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles.dart';
import '../auth/etat_authentification.dart';
import 'ecran_changement_mot_de_passe.dart';

/// Page d'accueil après connexion. Les écrans parents et élèves viendront s'y brancher aux étapes suivantes.
class EcranAccueil extends StatelessWidget {
  const EcranAccueil({super.key});

  @override
  Widget build(BuildContext context) {
    final moi = context.watch<EtatAuthentification>().moi!;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestion scolaire'),
        actions: [
          PopupMenuButton<String>(
            onSelected: (choix) {
              if (choix == 'mot_de_passe') {
                Navigator.of(context).push(
                  MaterialPageRoute<void>(builder: (_) => const EcranChangementMotDePasse()),
                );
              } else if (choix == 'deconnexion') {
                context.read<EtatAuthentification>().deconnexion();
              }
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'mot_de_passe', child: Text('Changer le mot de passe')),
              PopupMenuItem(value: 'deconnexion', child: Text('Déconnexion')),
            ],
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text('Bonjour', style: theme.textTheme.headlineSmall),
            const SizedBox(height: 4),
            Text(moi.email, style: theme.textTheme.bodyMedium),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [for (final role in moi.roles) Chip(label: Text(role.libelle))],
            ),
            const SizedBox(height: 24),
            if (!moi.estUtilisateurMobile)
              const _Carte(
                titre: 'Application réservée aux familles',
                texte: 'Cette application est destinée aux parents et aux élèves. '
                    'Pour votre rôle, utilisez le site web.',
              )
            else
              const _Carte(
                titre: 'Connexion réussie',
                texte: 'Les écrans (enfants, bulletins, présences, emploi du temps, factures) '
                    'arrivent aux prochaines étapes.',
              ),
          ],
        ),
      ),
    );
  }
}

class _Carte extends StatelessWidget {
  const _Carte({required this.titre, required this.texte});

  final String titre;
  final String texte;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(titre, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(texte),
          ],
        ),
      ),
    );
  }
}
