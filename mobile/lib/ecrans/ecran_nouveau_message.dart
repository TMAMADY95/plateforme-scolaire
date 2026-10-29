import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../api/modeles_messagerie.dart';
import '../api/modeles_scolaires.dart';
import '../api/service_messagerie.dart';
import '../api/service_scolaire.dart';
import '../widgets/chargeur_donnees.dart';

/// Un parent écrit à un enseignant ou à la direction à propos d'un de ses enfants.
class EcranNouveauMessage extends StatelessWidget {
  const EcranNouveauMessage({super.key});

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceScolaire>();
    return Scaffold(
      appBar: AppBar(title: const Text('Nouveau message')),
      body: ChargeurDonnees<List<Enfant>>(
        charger: service.mesEnfants,
        construire: (contexte, enfants) {
          final inscrits = enfants.where((e) => e.estInscrit).toList();
          if (inscrits.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text("Aucun enfant inscrit : il n'y a personne à contacter pour le moment."),
              ),
            );
          }
          return _Formulaire(enfants: inscrits);
        },
      ),
    );
  }
}

class _Formulaire extends StatefulWidget {
  const _Formulaire({required this.enfants});

  final List<Enfant> enfants;

  @override
  State<_Formulaire> createState() => _EtatFormulaire();
}

class _EtatFormulaire extends State<_Formulaire> {
  final _sujet = TextEditingController();
  final _contenu = TextEditingController();
  int? _eleveId;
  int? _destinataireId;
  List<Contact>? _contacts;
  String? _erreur;
  bool _envoi = false;
  int _generation = 0;

  @override
  void dispose() {
    _sujet.dispose();
    _contenu.dispose();
    super.dispose();
  }

  Future<void> _choisirEnfant(int? eleveId) async {
    if (eleveId == null) return;
    final generation = ++_generation;
    setState(() {
      _eleveId = eleveId;
      _destinataireId = null;
      _contacts = null;
      _erreur = null;
    });
    try {
      final contacts = await context.read<ServiceMessagerie>().contacts(eleveId);
      if (mounted && generation == _generation) setState(() => _contacts = contacts);
    } on ApiException catch (e) {
      if (mounted && generation == _generation) setState(() => _erreur = e.message);
    }
  }

  Future<void> _envoyer() async {
    if (_eleveId == null || _destinataireId == null || _sujet.text.trim().isEmpty || _contenu.text.trim().isEmpty) {
      setState(() => _erreur = 'Choisissez un enfant et un destinataire, puis renseignez le sujet et le message.');
      return;
    }
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await context.read<ServiceMessagerie>().demarrer(
            eleveId: _eleveId!,
            destinataireId: _destinataireId!,
            sujet: _sujet.text.trim(),
            contenu: _contenu.text.trim(),
          );
      if (mounted) Navigator.of(context).pop();
    } on ApiException catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final contacts = _contacts;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        DropdownMenu<int>(
          expandedInsets: EdgeInsets.zero,
          label: const Text('À propos de'),
          onSelected: _choisirEnfant,
          dropdownMenuEntries: [
            for (final e in widget.enfants) DropdownMenuEntry<int>(value: e.eleveId, label: e.nomComplet),
          ],
        ),
        const SizedBox(height: 16),
        if (_eleveId != null && contacts == null && _erreur == null)
          const Center(child: CircularProgressIndicator())
        else if (contacts != null && contacts.isEmpty)
          const Text('Personne à contacter pour le moment.')
        else if (contacts != null)
          DropdownMenu<int>(
            key: ValueKey(_eleveId),
            expandedInsets: EdgeInsets.zero,
            label: const Text('Destinataire'),
            onSelected: (v) => setState(() => _destinataireId = v),
            dropdownMenuEntries: [
              for (final c in contacts)
                DropdownMenuEntry<int>(value: c.utilisateurId, label: '${c.nom} — ${c.role}'),
            ],
          ),
        const SizedBox(height: 16),
        TextField(controller: _sujet, maxLength: 200, decoration: const InputDecoration(labelText: 'Sujet')),
        const SizedBox(height: 8),
        TextField(
          controller: _contenu,
          minLines: 4,
          maxLines: 10,
          maxLength: 4000,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(labelText: 'Message', alignLabelWithHint: true),
        ),
        if (_erreur != null) ...[
          const SizedBox(height: 8),
          Text(_erreur!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
        ],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _envoi ? null : _envoyer,
          child: _envoi
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Envoyer'),
        ),
      ],
    );
  }
}
