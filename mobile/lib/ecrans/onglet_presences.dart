import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modeles_scolaires.dart';
import '../api/service_scolaire.dart';
import '../utils/format.dart';
import '../widgets/chargeur_donnees.dart';

class OngletPresences extends StatefulWidget {
  const OngletPresences({super.key, required this.enfant, this.maintenant});

  final Enfant enfant;
  final DateTime? maintenant;

  @override
  State<OngletPresences> createState() => _EtatOngletPresences();
}

class _EtatOngletPresences extends State<OngletPresences> with AutomaticKeepAliveClientMixin {
  int _jours = 30;

  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final service = context.read<ServiceScolaire>();
    final fin = widget.maintenant ?? DateTime.now();
    final debut = fin.subtract(Duration(days: _jours));
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        SegmentedButton<int>(
          segments: const [
            ButtonSegment(value: 30, label: Text('30 jours')),
            ButtonSegment(value: 90, label: Text('90 jours')),
            ButtonSegment(value: 180, label: Text('180 jours')),
          ],
          selected: {_jours},
          onSelectionChanged: (s) => setState(() => _jours = s.first),
        ),
        const SizedBox(height: 16),
        ChargeurDonnees<List<PresenceEleve>>(
          cle: _jours,
          charger: () => service.presences(widget.enfant.eleveId, dateIso(debut), dateIso(fin)),
          construire: (_, presences) => _ContenuPresences(presences: presences),
        ),
      ],
    );
  }
}

class _ContenuPresences extends StatelessWidget {
  const _ContenuPresences({required this.presences});

  final List<PresenceEleve> presences;

  @override
  Widget build(BuildContext context) {
    final presents = presences.where((p) => p.statut == StatutPresence.present).length;
    final absences = presences.where((p) => p.statut == StatutPresence.absent).toList();
    final retards = presences.where((p) => p.statut == StatutPresence.retard).length;
    final justifiees = absences.where((p) => p.justifie).length;
    final anomalies = presences.where((p) => p.statut != StatutPresence.present).toList()
      ..sort((a, b) => b.date.compareTo(a.date));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            Chip(label: Text('Présent : $presents')),
            Chip(label: Text('Absences : ${absences.length} (dont $justifiees justifiée${justifiees > 1 ? 's' : ''})')),
            Chip(label: Text('Retards : $retards')),
          ],
        ),
        const SizedBox(height: 8),
        if (anomalies.isEmpty)
          const Padding(padding: EdgeInsets.all(16), child: Text('Aucune absence ni retard sur la période.'))
        else
          for (final p in anomalies)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Icon(
                p.statut == StatutPresence.absent ? Icons.event_busy : Icons.schedule,
                color: p.statut == StatutPresence.absent ? Theme.of(context).colorScheme.error : Colors.orange.shade800,
              ),
              title: Text('${p.statut.libelle} — ${formaterDate(p.date)}'),
              subtitle: Text([
                if (p.motif != null && p.motif!.isNotEmpty) p.motif!,
                if (p.statut == StatutPresence.absent) (p.justifie ? 'Justifiée' : 'Non justifiée'),
              ].join(' · ')),
            ),
      ],
    );
  }
}
