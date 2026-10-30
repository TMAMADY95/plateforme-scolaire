// Le point du jour sur chaque enfant (GET /api/me/resume).

class DevoirResume {
  const DevoirResume({required this.matiere, required this.titre, required this.dateLimite});

  final String matiere;
  final String titre;

  /// Au format AAAA-MM-JJ.
  final String dateLimite;
}

class ResumeEnfant {
  const ResumeEnfant({
    required this.eleveId,
    this.classeId,
    this.periode,
    this.moyenneGenerale,
    this.rang,
    this.effectif = 0,
    this.absencesSemaine = 0,
    this.absencesNonJustifiees = 0,
    this.retardsSemaine = 0,
    this.prochainDevoir,
    this.facturesImpayees = 0,
    this.facturesEnRetard = 0,
    this.resteAPayer = 0,
    this.devise,
  });

  final int eleveId;
  final int? classeId;
  final String? periode;
  final double? moyenneGenerale;
  final int? rang;
  final int effectif;
  final int absencesSemaine;
  final int absencesNonJustifiees;
  final int retardsSemaine;
  final DevoirResume? prochainDevoir;
  final int facturesImpayees;
  final int facturesEnRetard;
  /// null quand les factures sont dans des devises différentes (pas de total).
  final double? resteAPayer;
  final String? devise;

  factory ResumeEnfant.depuisJson(Map<String, dynamic> json) {
    final devoirs = (json['prochainsDevoirs'] as List<dynamic>?)?.whereType<Map<String, dynamic>>().toList() ?? const [];
    int entier(String cle) => (json[cle] as num?)?.toInt() ?? 0;
    return ResumeEnfant(
      eleveId: (json['eleveId'] as num).toInt(),
      classeId: (json['classeId'] as num?)?.toInt(),
      periode: json['periode'] as String?,
      moyenneGenerale: (json['moyenneGenerale'] as num?)?.toDouble(),
      rang: (json['rang'] as num?)?.toInt(),
      effectif: entier('effectif'),
      absencesSemaine: entier('absencesSemaine'),
      absencesNonJustifiees: entier('absencesNonJustifiees'),
      retardsSemaine: entier('retardsSemaine'),
      prochainDevoir: devoirs.isEmpty
          ? null
          : DevoirResume(
              matiere: devoirs.first['matiere'] as String? ?? '',
              titre: devoirs.first['titre'] as String? ?? '',
              dateLimite: devoirs.first['dateLimite'] as String? ?? '',
            ),
      facturesImpayees: entier('facturesImpayees'),
      facturesEnRetard: entier('facturesEnRetard'),
      resteAPayer: (json['resteAPayer'] as num?)?.toDouble(),
      devise: json['devise'] as String?,
    );
  }

  /// Réponse inattendue (ou absente) : liste vide, jamais d'erreur.
  static List<ResumeEnfant> listeDepuisJson(Object? json) => json is List
      ? json
          .whereType<Map<String, dynamic>>()
          .where((m) => m['eleveId'] is num)
          .map(ResumeEnfant.depuisJson)
          .toList()
      : <ResumeEnfant>[];
}
