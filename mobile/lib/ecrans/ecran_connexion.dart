import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../auth/etat_authentification.dart';

class EcranConnexion extends StatefulWidget {
  const EcranConnexion({super.key});

  @override
  State<EcranConnexion> createState() => _EtatEcranConnexion();
}

class _EtatEcranConnexion extends State<EcranConnexion> {
  final _formulaire = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();
  bool _masque = true;
  bool _enCours = false;
  String? _erreur;

  @override
  void dispose() {
    _email.dispose();
    _motDePasse.dispose();
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
      await etat.connexion(_email.text, _motDePasse.text);
    } on ApiException catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final message = context.watch<EtatAuthentification>().message;
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formulaire,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('Gestion scolaire', style: theme.textTheme.headlineMedium, textAlign: TextAlign.center),
                    const SizedBox(height: 8),
                    Text(
                      'Connectez-vous avec votre adresse e-mail.',
                      style: theme.textTheme.bodyMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 24),
                    if (message != null) ...[
                      _Bandeau(message: message, couleur: theme.colorScheme.secondaryContainer),
                      const SizedBox(height: 16),
                    ],
                    TextFormField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.username],
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(labelText: 'Adresse e-mail'),
                      validator: (v) => (v == null || !RegExp(r'^\S+@\S+$').hasMatch(v.trim()))
                          ? 'Adresse e-mail invalide'
                          : null,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _motDePasse,
                      obscureText: _masque,
                      autofillHints: const [AutofillHints.password],
                      textInputAction: TextInputAction.done,
                      onFieldSubmitted: (_) => _valider(),
                      decoration: InputDecoration(
                        labelText: 'Mot de passe',
                        suffixIcon: IconButton(
                          tooltip: _masque ? 'Afficher' : 'Masquer',
                          icon: Icon(_masque ? Icons.visibility : Icons.visibility_off),
                          onPressed: () => setState(() => _masque = !_masque),
                        ),
                      ),
                      validator: (v) => (v == null || v.isEmpty) ? 'Mot de passe requis' : null,
                    ),
                    if (_erreur != null) ...[
                      const SizedBox(height: 16),
                      _Bandeau(message: _erreur!, couleur: theme.colorScheme.errorContainer),
                    ],
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: _enCours ? null : _valider,
                      child: _enCours
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Text('Se connecter'),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      "Mot de passe oublié ? Contactez la direction de l'établissement : elle peut le réinitialiser.",
                      style: theme.textTheme.bodySmall,
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Bandeau extends StatelessWidget {
  const _Bandeau({required this.message, required this.couleur});

  final String message;
  final Color couleur;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: couleur, borderRadius: BorderRadius.circular(8)),
      child: Text(message),
    );
  }
}
