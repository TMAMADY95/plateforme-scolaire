import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_enseignant.dart';
import '../api/modeles_scolaires.dart';
import '../api/service_enseignant.dart';
import '../api/service_scolaire.dart';
import '../auth/etat_authentification.dart';
import '../theme.dart';
import '../widgets/chargeur_donnees.dart';
import 'ecran_changement_mot_de_passe.dart';
import 'ecran_classe.dart';
import 'ecran_enfant.dart';
import 'ecran_messages.dart';

/// Page d'accueil après connexion : la liste des enfants (parent) ou l'espace de l'élève.
class EcranAccueil extends StatelessWidget {
  const EcranAccueil({super.key});

  @override
  Widget build(BuildContext context) {
    final moi = context.watch<EtatAuthentification>().moi!;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text(nomApplication),
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
          padding: const EdgeInsets.all(16),
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [couleurMarine, Color(0xFF0F3C8A)],
                ),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Bonjour',
                    style: theme.textTheme.headlineSmall?.copyWith(color: Colors.white, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  Text(moi.email, style: theme.textTheme.bodyMedium?.copyWith(color: const Color(0xFFD6E2FB))),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final role in moi.roles)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: const Color(0x33FFFFFF),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            role.libelle,
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            if (!moi.estUtilisateurMobile)
              const _Carte(
                titre: 'Application réservée aux familles et aux enseignants',
                texte: 'Cette application est destinée aux parents, aux élèves et aux enseignants. '
                    'Pour votre rôle, utilisez le site web.',
              )
            else ...[
              if (moi.estParent || moi.estEnseignant) ...[
                TuileMessages(peutEcrire: moi.estParent),
                const SizedBox(height: 12),
              ],
              if (moi.estEnseignant) const _MesClasses(),
              if (moi.estParent || moi.estEleve) _MesEnfants(estParent: moi.estParent, estEleve: moi.estEleve),
            ],
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

/// Charge les enfants du parent et/ou le dossier de l'élève connecté.
class _MesEnfants extends StatelessWidget {
  const _MesEnfants({required this.estParent, required this.estEleve});

  final bool estParent;
  final bool estEleve;

  Future<List<Enfant>> _charger(ServiceScolaire service) async {
    final resultat = <Enfant>[];
    if (estEleve) resultat.add(await service.monProfil());
    if (estParent) resultat.addAll(await service.mesEnfants());
    return resultat;
  }

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceScolaire>();
    return ChargeurDonnees<List<Enfant>>(
      charger: () => _charger(service),
      construire: (contexte, enfants) {
        if (enfants.isEmpty) {
          return const _Carte(
            titre: 'Aucun enfant rattaché',
            texte: "Aucun élève n'est rattaché à votre compte. Rapprochez-vous de l'établissement.",
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(estParent && !estEleve ? 'Mes enfants' : 'Mon espace', style: Theme.of(contexte).textTheme.titleMedium),
            const SizedBox(height: 8),
            for (final enfant in enfants)
              Card(
                child: ListTile(
                  leading: _Initiales(texte: enfant.nomComplet),
                  title: Text(enfant.nomComplet),
                  subtitle: Text([
                    if (enfant.relation.isNotEmpty) enfant.relation,
                    enfant.descriptionClasse,
                  ].join('\n')),
                  isThreeLine: enfant.relation.isNotEmpty,
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(contexte).push(
                    MaterialPageRoute<void>(builder: (_) => EcranEnfant(enfant: enfant)),
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

/// Les classes de l'enseignant ; un appui ouvre la classe (appel).
class _MesClasses extends StatelessWidget {
  const _MesClasses();

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceEnseignant>();
    return ChargeurDonnees<List<ClasseEnseignee>>(
      charger: service.mesClasses,
      construire: (contexte, classes) {
        if (classes.isEmpty) {
          return const _Carte(
            titre: 'Aucune classe',
            texte: 'Aucune classe ne vous est affectée. La direction de votre établissement peut vous en affecter.',
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Mes classes', style: Theme.of(contexte).textTheme.titleMedium),
            const SizedBox(height: 8),
            for (final classe in classes)
              Card(
                child: ListTile(
                  leading: const _Pastille(icone: Icons.groups),
                  title: Text('Classe ${classe.nom}'),
                  subtitle: Text([
                    classe.niveau,
                    classe.anneeLibelle,
                    classe.matieres.join(', '),
                  ].where((t) => t.isNotEmpty).join(' — ')),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(contexte).push(
                    MaterialPageRoute<void>(builder: (_) => EcranClasse(classe: classe)),
                  ),
                ),
              ),
            const SizedBox(height: 16),
          ],
        );
      },
    );
  }
}

/// Pastille arrondie bleu clair avec une icône.
class _Pastille extends StatelessWidget {
  const _Pastille({required this.icone});

  final IconData icone;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(color: const Color(0xFFE6EEFC), borderRadius: BorderRadius.circular(12)),
      child: Icon(icone, color: couleurBleu),
    );
  }
}

/// Avatar à initiales (« Aminata Traoré » -> « AT »).
class _Initiales extends StatelessWidget {
  const _Initiales({required this.texte});

  final String texte;

  @override
  Widget build(BuildContext context) {
    final mots = texte.trim().split(RegExp(r'\s+')).where((m) => m.isNotEmpty).toList();
    final initiales = mots.take(2).map((m) => m.substring(0, 1).toUpperCase()).join();
    return CircleAvatar(
      backgroundColor: const Color(0xFFE6EEFC),
      foregroundColor: couleurBleu,
      child: Text(initiales, style: const TextStyle(fontWeight: FontWeight.w700)),
    );
  }
}
