import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import '../utils/format.dart';
import '../widgets/bouton_pdf.dart';
import '../widgets/chargeur_donnees.dart';

Future<bool> _ouvrirDansLeNavigateur(Uri url) => launchUrl(url, mode: LaunchMode.externalApplication);

/// Factures d'un enfant et paiement en ligne. Le paiement se termine dans le navigateur ; au retour dans
/// l'application, la liste se recharge toute seule.
class OngletFactures extends StatefulWidget {
  const OngletFactures({super.key, required this.enfant, this.ouvrirUrl = _ouvrirDansLeNavigateur});

  final Enfant enfant;

  /// Ouvre l'adresse du prestataire de paiement (remplaçable dans les tests).
  final Future<bool> Function(Uri url) ouvrirUrl;

  @override
  State<OngletFactures> createState() => _EtatOngletFactures();
}

class _EtatOngletFactures extends State<OngletFactures> with AutomaticKeepAliveClientMixin, WidgetsBindingObserver {
  int _version = 0;
  int? _enCours;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState etat) {
    if (etat == AppLifecycleState.resumed && mounted) setState(() => _version++);
  }

  Future<void> _payer(Facture facture) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (contexte) => AlertDialog(
        title: const Text('Payer en ligne'),
        content: Text('Payer ${formaterMontant(facture.resteAPayer, facture.devise)} pour « ${facture.libelle} » ?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(contexte, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(contexte, true), child: const Text('Payer')),
        ],
      ),
    );
    if (confirme != true || !mounted) return;

    final service = context.read<ServiceScolaire>();
    final messager = ScaffoldMessenger.of(context);
    setState(() => _enCours = facture.id);
    try {
      final initie = await service.payerEnLigne(facture.id);
      final ouvert = await widget.ouvrirUrl(Uri.parse(initie.urlPaiement));
      if (!ouvert) messager.showSnackBar(const SnackBar(content: Text("Impossible d'ouvrir la page de paiement.")));
    } on ApiException catch (e) {
      messager.showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _enCours = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final service = context.read<ServiceScolaire>();
    return ChargeurDonnees<List<Facture>>(
      cle: _version,
      charger: () => service.factures(widget.enfant),
      construire: (contexte, factures) {
        if (factures.isEmpty) {
          return const Center(child: Padding(padding: EdgeInsets.all(24), child: Text('Aucune facture pour le moment.')));
        }
        final theme = Theme.of(contexte);
        final reste = factures.fold<double>(0, (somme, f) => somme + f.resteAPayer);
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Reste à payer : ${formaterMontant(reste, factures.first.devise)}',
                    style: theme.textTheme.titleMedium?.copyWith(color: reste > 0 ? theme.colorScheme.error : null),
                  ),
                ),
                IconButton(
                  tooltip: 'Actualiser',
                  icon: const Icon(Icons.refresh),
                  onPressed: () => setState(() => _version++),
                ),
              ],
            ),
            for (final f in factures)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(f.libelle, style: theme.textTheme.titleSmall),
                      Text(f.numero, style: theme.textTheme.bodySmall),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: [
                          Chip(label: Text(f.statut.libelle)),
                          if (f.enRetard)
                            Chip(
                              label: Text('En retard', style: TextStyle(color: theme.colorScheme.onErrorContainer)),
                              backgroundColor: theme.colorScheme.errorContainer,
                            ),
                        ],
                      ),
                      Text('Montant dû : ${formaterMontant(f.montantDu, f.devise)}'),
                      Text('Déjà payé : ${formaterMontant(f.montantPaye, f.devise)}'),
                      Text('Reste : ${formaterMontant(f.resteAPayer, f.devise)}'),
                      if (f.echeance != null) Text('Échéance : ${formaterDate(f.echeance!)}'),
                      if (f.montantPaye > 0)
                        Align(
                          alignment: Alignment.centerLeft,
                          child: BoutonPdf(
                            libelle: 'Reçu (PDF)',
                            nomFichier: 'recu-${f.numero}.pdf',
                            telecharger: () => service.recuPdf(f.id),
                          ),
                        ),
                      if (f.payable) ...[
                        const SizedBox(height: 8),
                        Align(
                          alignment: Alignment.centerRight,
                          child: FilledButton(
                            onPressed: _enCours == null ? () => _payer(f) : null,
                            child: _enCours == f.id
                                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                                : const Text('Payer'),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 8),
            Text(
              "Le paiement en ligne n'est peut-être pas activé : dans ce cas, rapprochez-vous de l'établissement.",
              style: theme.textTheme.bodySmall,
            ),
          ],
        );
      },
    );
  }
}
