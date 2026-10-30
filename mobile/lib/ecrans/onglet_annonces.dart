import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/modeles_scolaires.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';
import '../widgets/formulaires_publication.dart';

/// Annonces de la classe. Avec [publier] (enseignant), un bouton permet d'en écrire une ;
/// avec [supprimer], l'auteur peut retirer les siennes.
class OngletAnnonces extends StatefulWidget {
  const OngletAnnonces({
    super.key,
    required this.charger,
    this.messageVide = 'Aucune annonce pour le moment.',
    this.publier,
    this.supprimer,
  });

  final Future<List<Annonce>> Function() charger;
  final String messageVide;
  final Future<void> Function(String titre, String contenu)? publier;
  final Future<void> Function(Annonce annonce)? supprimer;

  @override
  State<OngletAnnonces> createState() => _EtatOngletAnnonces();
}

class _EtatOngletAnnonces extends State<OngletAnnonces> with AutomaticKeepAliveClientMixin {
  int _version = 0;

  @override
  bool get wantKeepAlive => true;

  void _message(String texte) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texte)));

  Future<void> _executer(Future<void> Function() action, String succes) async {
    try {
      await action();
      if (!mounted) return;
      setState(() => _version++);
      _message(succes);
    } on ApiException catch (e) {
      if (mounted) _message(e.message);
    }
  }

  Future<void> _ecrire() async {
    final saisie = await demanderAnnonce(context);
    if (saisie == null || !mounted) return;
    await _executer(() => widget.publier!(saisie.titre, saisie.contenu), 'Annonce publiée.');
  }

  Future<void> _retirer(Annonce annonce) async {
    if (!await confirmer(context, 'Supprimer l\'annonce « ${annonce.titre} » ?') || !mounted) return;
    await _executer(() => widget.supprimer!(annonce), 'Annonce supprimée.');
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final liste = ChargeurDonnees<List<Annonce>>(
      charger: widget.charger,
      cle: _version,
      construire: (contexte, annonces) {
        if (annonces.isEmpty) {
          return Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(widget.messageVide)));
        }
        final theme = Theme.of(contexte);
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            for (final a in annonces)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(child: Text(a.titre, style: theme.textTheme.titleMedium)),
                          if (widget.supprimer != null && a.modifiable)
                            IconButton(
                              tooltip: 'Supprimer l\'annonce',
                              icon: const Icon(Icons.delete_outline),
                              onPressed: () => _retirer(a),
                            ),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text('${a.auteur} · ${formaterHorodatage(a.publieLe)}', style: theme.textTheme.bodySmall),
                      const SizedBox(height: 8),
                      Text(a.contenu),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
    if (widget.publier == null) return liste;
    return Column(
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: FilledButton.icon(onPressed: _ecrire, icon: const Icon(Icons.add), label: const Text('Nouvelle annonce')),
          ),
        ),
        Expanded(child: liste),
      ],
    );
  }
}
