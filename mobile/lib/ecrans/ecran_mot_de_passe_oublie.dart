import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../auth/etat_authentification.dart';

/// Demande d'un lien de réinitialisation par e-mail. Le lien s'ouvre sur le site web, où l'on choisit
/// le nouveau mot de passe ; on revient ensuite ici pour se connecter.
class EcranMotDePasseOublie extends StatefulWidget {
  const EcranMotDePasseOublie({super.key, this.emailInitial = ''});

  final String emailInitial;

  @override
  State<EcranMotDePasseOublie> createState() => _EtatEcranMotDePasseOublie();
}

class _EtatEcranMotDePasseOublie extends State<EcranMotDePasseOublie> {
  final _formulaire = GlobalKey<FormState>();
  late final TextEditingController _email = TextEditingController(text: widget.emailInitial);
  bool _enCours = false;
  bool _envoye = false;
  String? _erreur;

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _envoyer() async {
    if (!_formulaire.currentState!.validate()) return;
    final api = context.read<EtatAuthentification>().api;
    setState(() {
      _enCours = true;
      _erreur = null;
    });
    try {
      await api.post('/api/auth/mot-de-passe-oublie', corps: {'email': _email.text.trim()}, sansJeton: true);
      if (mounted) setState(() => _envoye = true);
    } on ApiException catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Mot de passe oublié')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: _envoye
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Text(
                          'Si cette adresse correspond à un compte, un e-mail vient de lui être envoyé. '
                          'Ouvrez le lien qu\'il contient (valable 30 minutes) pour choisir un nouveau mot de passe, '
                          'puis revenez vous connecter ici. Pensez à regarder dans les courriers indésirables.',
                          style: theme.textTheme.bodyMedium,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    FilledButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Retour à la connexion')),
                  ],
                )
              : Form(
                  key: _formulaire,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text('Indiquez l\'adresse e-mail de votre compte : nous vous envoyons un lien pour en choisir un nouveau.',
                          style: theme.textTheme.bodyMedium),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.username],
                        decoration: const InputDecoration(labelText: 'Adresse e-mail'),
                        validator: (v) => (v == null || !RegExp(r'^\S+@\S+$').hasMatch(v.trim()))
                            ? 'Adresse e-mail invalide'
                            : null,
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
                        onPressed: _enCours ? null : _envoyer,
                        child: _enCours
                            ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                            : const Text('Envoyer le lien'),
                      ),
                    ],
                  ),
                ),
        ),
      ),
    );
  }
}
