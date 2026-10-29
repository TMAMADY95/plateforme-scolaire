import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_client.dart';
import '../api/modeles_messagerie.dart';
import '../api/service_messagerie.dart';
import '../utils/format.dart';

/// Un fil de discussion : messages, zone de réponse. L'ouverture marque la conversation comme lue.
class EcranConversation extends StatefulWidget {
  const EcranConversation({super.key, required this.conversationId});

  final int conversationId;

  @override
  State<EcranConversation> createState() => _EtatEcranConversation();
}

class _EtatEcranConversation extends State<EcranConversation> {
  final _saisie = TextEditingController();
  final _defilement = ScrollController();
  Conversation? _conversation;
  String? _erreur;
  bool _envoi = false;
  Timer? _minuteur;

  @override
  void initState() {
    super.initState();
    _charger();
    // Nouveaux messages de l'autre personne : relecture régulière tant que l'écran est ouvert.
    _minuteur = Timer.periodic(const Duration(seconds: 20), (_) => _charger(silencieux: true));
  }

  @override
  void dispose() {
    _minuteur?.cancel();
    _saisie.dispose();
    _defilement.dispose();
    super.dispose();
  }

  Future<void> _charger({bool silencieux = false}) async {
    try {
      final c = await context.read<ServiceMessagerie>().ouvrir(widget.conversationId);
      if (!mounted) return;
      setState(() {
        _conversation = c;
        if (!silencieux) _erreur = null;
      });
      _descendre();
    } on ApiException catch (e) {
      if (mounted && !silencieux) setState(() => _erreur = e.message);
    }
  }

  void _descendre() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_defilement.hasClients) _defilement.jumpTo(_defilement.position.maxScrollExtent);
    });
  }

  Future<void> _envoyer() async {
    final texte = _saisie.text.trim();
    if (texte.isEmpty || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      final c = await context.read<ServiceMessagerie>().repondre(widget.conversationId, texte);
      if (!mounted) return;
      _saisie.clear();
      setState(() => _conversation = c);
      _descendre();
    } on ApiException catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = _conversation;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(c?.sujet ?? 'Conversation')),
      body: c == null
          ? Center(
              child: _erreur != null
                  ? Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(_erreur!, textAlign: TextAlign.center),
                          const SizedBox(height: 12),
                          FilledButton.tonal(onPressed: _charger, child: const Text('Réessayer')),
                        ],
                      ),
                    )
                  : const CircularProgressIndicator(),
            )
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      '${c.interlocuteur} (${c.roleInterlocuteur}) · à propos de ${c.eleve}',
                      style: theme.textTheme.bodySmall,
                    ),
                  ),
                ),
                Expanded(
                  child: ListView(
                    controller: _defilement,
                    padding: const EdgeInsets.all(16),
                    children: [
                      for (final m in c.messages)
                        Align(
                          alignment: m.moi ? Alignment.centerRight : Alignment.centerLeft,
                          child: ConstrainedBox(
                            constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.8),
                            child: Container(
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: m.moi ? const Color(0xFFDCE8FF) : const Color(0xFFEEF1F6),
                                borderRadius: BorderRadius.circular(14),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${m.moi ? 'Moi' : m.auteur} · ${formaterHorodatage(m.envoyeLe)}',
                                    style: theme.textTheme.bodySmall,
                                  ),
                                  const SizedBox(height: 4),
                                  Text(m.contenu),
                                ],
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                if (_erreur != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Text(_erreur!, style: TextStyle(color: theme.colorScheme.error)),
                  ),
                if (c.active)
                  SafeArea(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _saisie,
                              minLines: 1,
                              maxLines: 5,
                              maxLength: 4000,
                              textCapitalization: TextCapitalization.sentences,
                              decoration: const InputDecoration(hintText: 'Votre réponse', counterText: ''),
                            ),
                          ),
                          IconButton(
                            onPressed: _envoi ? null : _envoyer,
                            icon: const Icon(Icons.send),
                            tooltip: 'Envoyer',
                          ),
                        ],
                      ),
                    ),
                  )
                else
                  const SafeArea(
                    child: Padding(
                      padding: EdgeInsets.all(16),
                      child: Text("Cette conversation n'est plus active : vous pouvez la relire mais plus y répondre."),
                    ),
                  ),
              ],
            ),
    );
  }
}
