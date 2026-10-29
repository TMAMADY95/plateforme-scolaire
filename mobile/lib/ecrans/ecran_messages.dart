import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_messagerie.dart';
import '../api/service_messagerie.dart';
import '../theme.dart';
import '../widgets/chargeur_donnees.dart';
import 'ecran_conversation.dart';
import 'ecran_nouveau_message.dart';

/// Liste des conversations. Seul un parent peut en démarrer une ; les enseignants et la direction répondent
/// (ils écrivent aux parents depuis l'application web).
class EcranMessages extends StatefulWidget {
  const EcranMessages({super.key, required this.peutEcrire});

  final bool peutEcrire;

  @override
  State<EcranMessages> createState() => _EtatEcranMessages();
}

class _EtatEcranMessages extends State<EcranMessages> {
  int _version = 0;

  void _actualiser() {
    if (mounted) setState(() => _version++);
  }

  @override
  Widget build(BuildContext context) {
    final service = context.read<ServiceMessagerie>();
    return Scaffold(
      appBar: AppBar(title: const Text('Messages')),
      floatingActionButton: widget.peutEcrire
          ? FloatingActionButton.extended(
              onPressed: () async {
                await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const EcranNouveauMessage()));
                _actualiser();
              },
              icon: const Icon(Icons.edit),
              label: const Text('Nouveau'),
            )
          : null,
      body: ChargeurDonnees<List<Conversation>>(
        cle: _version,
        charger: service.conversations,
        construire: (contexte, conversations) {
          if (conversations.isEmpty) {
            return const Center(
              child: Padding(padding: EdgeInsets.all(24), child: Text('Aucune conversation pour le moment.')),
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
            children: [
              for (final c in conversations)
                Card(
                  child: ListTile(
                    title: Text(c.interlocuteur, style: TextStyle(fontWeight: c.nonLu ? FontWeight.w700 : FontWeight.w500)),
                    subtitle: Text('${c.sujet}\n${c.eleve} · ${c.apercu}', maxLines: 3, overflow: TextOverflow.ellipsis),
                    isThreeLine: true,
                    trailing: c.nonLu ? const Icon(Icons.circle, size: 12, color: couleurBleu) : null,
                    onTap: () async {
                      await Navigator.of(contexte).push(
                        MaterialPageRoute<void>(builder: (_) => EcranConversation(conversationId: c.id)),
                      );
                      _actualiser();
                    },
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

/// Tuile « Messages » de l'accueil, avec le nombre de conversations non lues. Discrète en cas d'erreur.
class TuileMessages extends StatefulWidget {
  const TuileMessages({super.key, required this.peutEcrire});

  final bool peutEcrire;

  @override
  State<TuileMessages> createState() => _EtatTuileMessages();
}

class _EtatTuileMessages extends State<TuileMessages> {
  late Future<int> _nonLus;

  @override
  void initState() {
    super.initState();
    _nonLus = _charger();
  }

  Future<int> _charger() async {
    try {
      return await context.read<ServiceMessagerie>().nonLus();
    } catch (_) {
      return 0;
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<int>(
      future: _nonLus,
      builder: (contexte, instantane) {
        final n = instantane.data ?? 0;
        return Card(
          child: ListTile(
            leading: Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(color: const Color(0xFFE6EEFC), borderRadius: BorderRadius.circular(12)),
              child: const Icon(Icons.forum_outlined, color: couleurBleu),
            ),
            title: const Text('Messages'),
            subtitle: Text(n == 0 ? 'Aucun message non lu' : '$n conversation${n > 1 ? 's' : ''} non lue${n > 1 ? 's' : ''}'),
            trailing: n > 0 ? Badge.count(count: n) : const Icon(Icons.chevron_right),
            onTap: () async {
              await Navigator.of(contexte).push(
                MaterialPageRoute<void>(builder: (_) => EcranMessages(peutEcrire: widget.peutEcrire)),
              );
              if (mounted) setState(() => _nonLus = _charger());
            },
          ),
        );
      },
    );
  }
}
