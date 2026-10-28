import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../auth/etat_authentification.dart';

/// Changement de mot de passe. [obligatoire] : première connexion avec un mot de passe initial,
/// l'utilisateur ne peut rien faire d'autre (pas de retour possible).
class EcranChangementMotDePasse extends StatefulWidget {
  const EcranChangementMotDePasse({super.key, this.obligatoire = false});

  final bool obligatoire;

  @override
  State<EcranChangementMotDePasse> createState() => _EtatEcranChangement();
}

class _EtatEcranChangement extends State<EcranChangementMotDePasse> {
  final _formulaire = GlobalKey<FormState>();
  final _actuel = TextEditingController();
  final _nouveau = TextEditingController();
  final _confirmation = TextEditingController();
  bool _enCours = false;
  String? _erreur;

  @override
  void dispose() {
    _actuel.dispose();
    _nouveau.dispose();
    _confirmation.dispose();
    super.dispose();
  }

  Future<void> _valider() async {
    if (!_formulaire.currentState!.validate()) return;
    final etat = context.read<EtatAuthentification>();
    setState(() {
      _enCours = true;
      _erreur = null;
    });
    try {
      await etat.changerMotDePasse(_actuel.text, _nouveau.text);
      // Succès : la session est fermée, l'écran de connexion prend le relais.
    } on ApiException catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return PopScope(
      canPop: !widget.obligatoire,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Changer le mot de passe'),
          automaticallyImplyLeading: !widget.obligatoire,
          actions: [
            if (widget.obligatoire)
              TextButton(
                onPressed: () => context.read<EtatAuthentification>().deconnexion(),
                child: const Text('Déconnexion'),
              ),
          ],
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formulaire,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (widget.obligatoire) ...[
                    Text(
                      'Pour votre sécurité, remplacez le mot de passe qui vous a été remis avant de continuer.',
                      style: theme.textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 16),
                  ],
                  TextFormField(
                    controller: _actuel,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: 'Mot de passe actuel'),
                    validator: (v) => (v == null || v.isEmpty) ? 'Mot de passe actuel requis' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _nouveau,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'Nouveau mot de passe',
                      helperText: 'Au moins 10 caractères',
                    ),
                    validator: (v) {
                      if (v == null || v.length < 10) return 'Au moins 10 caractères';
                      if (v == _actuel.text) return "Il doit être différent de l'actuel";
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _confirmation,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: 'Confirmer le nouveau mot de passe'),
                    validator: (v) => v == _nouveau.text ? null : 'Les deux mots de passe diffèrent',
                  ),
                  if (_erreur != null) ...[
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.errorContainer,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(_erreur!),
                    ),
                  ],
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _enCours ? null : _valider,
                    child: _enCours
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Text('Enregistrer'),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Vous serez déconnecté et devrez vous reconnecter avec le nouveau mot de passe.',
                    style: theme.textTheme.bodySmall,
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
