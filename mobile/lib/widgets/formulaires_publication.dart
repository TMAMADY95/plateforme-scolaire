import 'package:flutter/material.dart';

import '../api/modeles_enseignant.dart';
import '../utils/format.dart';

/// Demande le titre et le texte d'une annonce ; null si l'utilisateur annule.
Future<({String titre, String contenu})?> demanderAnnonce(BuildContext contexte) =>
    showDialog<({String titre, String contenu})>(context: contexte, builder: (_) => const _DialogueAnnonce());

/// Demande les informations d'un devoir ; null si l'utilisateur annule.
Future<({int matiereId, String titre, String consigne, String dateLimite})?> demanderDevoir(
  BuildContext contexte, {
  required List<MatiereEnseignee> matieres,
  required DateTime aujourdhui,
}) =>
    showDialog<({int matiereId, String titre, String consigne, String dateLimite})>(
      context: contexte,
      builder: (_) => _DialogueDevoir(matieres: matieres, aujourdhui: aujourdhui),
    );

/// Demande une confirmation avant une suppression.
Future<bool> confirmer(BuildContext contexte, String question) async {
  final reponse = await showDialog<bool>(
    context: contexte,
    builder: (c) => AlertDialog(
      content: Text(question),
      actions: [
        TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Annuler')),
        FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('Supprimer')),
      ],
    ),
  );
  return reponse ?? false;
}

class _DialogueAnnonce extends StatefulWidget {
  const _DialogueAnnonce();

  @override
  State<_DialogueAnnonce> createState() => _EtatDialogueAnnonce();
}

class _EtatDialogueAnnonce extends State<_DialogueAnnonce> {
  final _cle = GlobalKey<FormState>();
  final _titre = TextEditingController();
  final _contenu = TextEditingController();

  @override
  void dispose() {
    _titre.dispose();
    _contenu.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Nouvelle annonce'),
      content: SingleChildScrollView(
        child: Form(
          key: _cle,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _titre,
                maxLength: 200,
                decoration: const InputDecoration(labelText: 'Titre'),
                validator: (v) => (v ?? '').trim().isEmpty ? 'Le titre est obligatoire.' : null,
              ),
              TextFormField(
                controller: _contenu,
                maxLength: 4000,
                minLines: 3,
                maxLines: 6,
                decoration: const InputDecoration(labelText: 'Message'),
                validator: (v) => (v ?? '').trim().isEmpty ? 'Le message est obligatoire.' : null,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
        FilledButton(
          onPressed: () {
            if (_cle.currentState!.validate()) {
              Navigator.pop(context, (titre: _titre.text.trim(), contenu: _contenu.text.trim()));
            }
          },
          child: const Text('Publier'),
        ),
      ],
    );
  }
}

class _DialogueDevoir extends StatefulWidget {
  const _DialogueDevoir({required this.matieres, required this.aujourdhui});

  final List<MatiereEnseignee> matieres;
  final DateTime aujourdhui;

  @override
  State<_DialogueDevoir> createState() => _EtatDialogueDevoir();
}

class _EtatDialogueDevoir extends State<_DialogueDevoir> {
  final _cle = GlobalKey<FormState>();
  final _titre = TextEditingController();
  final _consigne = TextEditingController();
  late int? _matiereId = widget.matieres.length == 1 ? widget.matieres.first.id : null;
  late DateTime _date = DateTime(widget.aujourdhui.year, widget.aujourdhui.month, widget.aujourdhui.day + 1);
  bool _matiereManquante = false;

  @override
  void dispose() {
    _titre.dispose();
    _consigne.dispose();
    super.dispose();
  }

  Future<void> _choisirDate() async {
    final jour = DateTime(widget.aujourdhui.year, widget.aujourdhui.month, widget.aujourdhui.day);
    final choix = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: jour,
      lastDate: jour.add(const Duration(days: 365)),
    );
    if (choix != null) setState(() => _date = choix);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Nouveau devoir'),
      content: SingleChildScrollView(
        child: Form(
          key: _cle,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (widget.matieres.length > 1) ...[
                DropdownMenu<int>(
                  label: const Text('Matière'),
                  expandedInsets: EdgeInsets.zero,
                  dropdownMenuEntries: [
                    for (final m in widget.matieres) DropdownMenuEntry(value: m.id, label: m.libelle),
                  ],
                  errorText: _matiereManquante ? 'Choisissez une matière.' : null,
                  onSelected: (v) => setState(() {
                    _matiereId = v;
                    _matiereManquante = false;
                  }),
                ),
                const SizedBox(height: 8),
              ] else if (widget.matieres.length == 1)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Text('Matière : ${widget.matieres.first.libelle}'),
                ),
              TextFormField(
                controller: _titre,
                maxLength: 200,
                decoration: const InputDecoration(labelText: 'Titre'),
                validator: (v) => (v ?? '').trim().isEmpty ? 'Le titre est obligatoire.' : null,
              ),
              TextFormField(
                controller: _consigne,
                maxLength: 4000,
                minLines: 2,
                maxLines: 5,
                decoration: const InputDecoration(labelText: 'Consigne (facultatif)'),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _choisirDate,
                icon: const Icon(Icons.event),
                label: Text('Pour le ${formaterDate(dateIso(_date))}'),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
        FilledButton(
          onPressed: () {
            final valide = _cle.currentState!.validate();
            if (_matiereId == null) {
              setState(() => _matiereManquante = true);
              return;
            }
            if (valide) {
              Navigator.pop(context, (
                matiereId: _matiereId!,
                titre: _titre.text.trim(),
                consigne: _consigne.text.trim(),
                dateLimite: dateIso(_date),
              ));
            }
          },
          child: const Text('Publier'),
        ),
      ],
    );
  }
}
