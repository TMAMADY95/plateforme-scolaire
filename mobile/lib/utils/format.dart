import '../api/modeles_scolaires.dart';

/// 15.333 -> "15,33" ; null -> "—".
String formaterNote(double? valeur) => valeur == null ? '—' : valeur.toStringAsFixed(2).replaceAll('.', ',');

/// 3.0 -> "3" ; 2.5 -> "2,5".
String formaterCoefficient(double valeur) =>
    (valeur == valeur.roundToDouble() ? valeur.toStringAsFixed(0) : valeur.toString()).replaceAll('.', ',');

/// Écart signé entre deux moyennes : "+1,20" ou "-0,50" ; "—" si l'une manque.
String formaterEcart(double? ecart) {
  if (ecart == null) return '—';
  final texte = ecart.toStringAsFixed(2).replaceAll('.', ',');
  return ecart > 0 ? '+$texte' : texte;
}

/// Libellé court du point du jour sur les présences : « Aucune absence », « 2 absences (1 à justifier) · 1 retard ».
String resumePresences(int absences, int nonJustifiees, int retards) {
  if (absences == 0 && retards == 0) return 'Aucune absence';
  final parties = <String>[
    if (absences > 0) '$absences absence${absences > 1 ? 's' : ''}${nonJustifiees > 0 ? ' ($nonJustifiees à justifier)' : ''}',
    if (retards > 0) '$retards retard${retards > 1 ? 's' : ''}',
  ];
  return parties.join(' · ');
}

/// 1 -> "1er", 2 -> "2e".
String formaterRang(int rang) => rang == 1 ? '1er' : '${rang}e';

/// "2026-10-26" -> "26/10/2026" (sans passer par DateTime : pas de décalage de fuseau horaire).
String formaterDate(String iso) => iso.split('-').reversed.join('/');

/// « 29/10 14:05 » à partir d'un horodatage ISO 8601 (affiché dans le fuseau de l'appareil).
String formaterHorodatage(String iso) {
  final d = DateTime.tryParse(iso)?.toLocal();
  if (d == null) return '';
  String deux(int n) => n.toString().padLeft(2, '0');
  return '${deux(d.day)}/${deux(d.month)} ${deux(d.hour)}:${deux(d.minute)}';
}

/// Date au format AAAA-MM-JJ, dans le fuseau local.
String dateIso(DateTime date) {
  final mois = date.month.toString().padLeft(2, '0');
  final jour = date.day.toString().padLeft(2, '0');
  return '${date.year}-$mois-$jour';
}

/// "08:00:00" -> "08:00".
String formaterHeure(String heure) => heure.length >= 5 ? heure.substring(0, 5) : heure;

const List<String> joursSemaine = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];

String libelleJour(int jourSemaine) =>
    (jourSemaine >= 1 && jourSemaine <= 7) ? joursSemaine[jourSemaine - 1] : 'Jour $jourSemaine';

/// Période en cours si elle existe, sinon la dernière ; null s'il n'y en a aucune.
Periode? periodeParDefaut(List<Periode> periodes, DateTime maintenant) {
  if (periodes.isEmpty) return null;
  final jour = dateIso(maintenant);
  for (final p in periodes) {
    if (p.dateDebut.compareTo(jour) <= 0 && jour.compareTo(p.dateFin) <= 0) return p;
  }
  return periodes.last;
}

const Set<String> _devisesSansDecimales = {'XOF', 'XAF', 'JPY', 'KRW', 'GNF', 'RWF', 'UGX', 'VND', 'KMF', 'DJF'};

/// 12500 + XOF -> "12 500 XOF" ; 12.5 + EUR -> "12,50 €". Le nombre de décimales suit la devise.
String formaterMontant(double montant, String devise) {
  final decimales = _devisesSansDecimales.contains(devise) ? 0 : 2;
  final fixe = montant.abs().toStringAsFixed(decimales);
  final parties = fixe.split('.');
  final entier = parties[0].replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => '\u202f');
  final nombre = decimales == 0 ? entier : '$entier,${parties[1]}';
  final signe = montant < 0 ? '-' : '';
  final symbole = devise == 'EUR' ? '€' : devise;
  return '$signe$nombre\u00a0$symbole'.trim();
}
