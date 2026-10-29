// Données de l'espace enseignant : classes enseignées et feuille d'appel.
import 'modeles_scolaires.dart';

/// Une affectation brute de GET /api/me/affectations : une matière dans une classe.
class AffectationEnseignant {
  const AffectationEnseignant({
    required this.classeId,
    required this.classeNom,
    required this.niveau,
    required this.anneeScolaireId,
    required this.anneeLibelle,
    required this.matiere,
  });

  final int classeId;
  final String classeNom;
  final String niveau;
  final int anneeScolaireId;
  final String anneeLibelle;
  final String matiere;

  factory AffectationEnseignant.depuisJson(Map<String, dynamic> json) => AffectationEnseignant(
        classeId: (json['classeId'] as num).toInt(),
        classeNom: json['classeNom'] as String,
        niveau: json['niveauLibelle'] as String? ?? '',
        anneeScolaireId: (json['anneeScolaireId'] as num).toInt(),
        anneeLibelle: json['anneeLibelle'] as String? ?? '',
        matiere: json['matiereLibelle'] as String? ?? '',
      );
}

/// Une classe, avec les matières que l'enseignant y assure.
class ClasseEnseignee {
  ClasseEnseignee({
    required this.classeId,
    required this.nom,
    required this.niveau,
    required this.anneeScolaireId,
    required this.anneeLibelle,
  });

  final int classeId;
  final String nom;
  final String niveau;
  final int anneeScolaireId;
  final String anneeLibelle;
  final List<String> matieres = [];

  /// Regroupe les affectations par classe ; les années les plus récentes d'abord, puis l'ordre alphabétique.
  static List<ClasseEnseignee> grouper(List<AffectationEnseignant> affectations) {
    final parClasse = <int, ClasseEnseignee>{};
    for (final a in affectations) {
      final classe = parClasse.putIfAbsent(
        a.classeId,
        () => ClasseEnseignee(
          classeId: a.classeId,
          nom: a.classeNom,
          niveau: a.niveau,
          anneeScolaireId: a.anneeScolaireId,
          anneeLibelle: a.anneeLibelle,
        ),
      );
      if (a.matiere.isNotEmpty && !classe.matieres.contains(a.matiere)) classe.matieres.add(a.matiere);
    }
    final classes = parClasse.values.toList()
      ..sort((a, b) {
        final parAnnee = b.anneeScolaireId.compareTo(a.anneeScolaireId);
        return parAnnee != 0 ? parAnnee : a.nom.compareTo(b.nom);
      });
    return classes;
  }
}

class LigneFeuille {
  const LigneFeuille({
    required this.inscriptionId,
    required this.nom,
    required this.prenom,
    required this.matricule,
    this.statut,
    this.motif,
    this.justifie = false,
  });

  final int inscriptionId;
  final String nom;
  final String prenom;
  final String matricule;

  /// null tant que l'appel n'a pas été fait pour cet élève.
  final StatutPresence? statut;
  final String? motif;
  final bool justifie;

  factory LigneFeuille.depuisJson(Map<String, dynamic> json) {
    final eleve = json['eleve'] as Map<String, dynamic>;
    final code = json['statut'] as String?;
    return LigneFeuille(
      inscriptionId: (json['inscriptionId'] as num).toInt(),
      nom: eleve['nom'] as String,
      prenom: eleve['prenom'] as String,
      matricule: eleve['matricule'] as String,
      statut: code == null ? null : StatutPresence.depuisCode(code),
      motif: json['motif'] as String?,
      justifie: json['justifie'] as bool? ?? false,
    );
  }

  String get nomComplet => '$nom $prenom';
}

class FeuilleAppel {
  const FeuilleAppel({required this.classeId, required this.date, required this.lignes});

  final int classeId;
  final String date;
  final List<LigneFeuille> lignes;

  factory FeuilleAppel.depuisJson(Map<String, dynamic> json) => FeuilleAppel(
        classeId: (json['classeId'] as num).toInt(),
        date: json['date'] as String,
        lignes: (json['lignes'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(LigneFeuille.depuisJson)
            .toList(),
      );
}

/// Une ligne envoyée au serveur : motif et justification ne valent que pour une absence ou un retard.
class LigneAppel {
  const LigneAppel({required this.inscriptionId, required this.statut, this.motif, this.justifie = false});

  final int inscriptionId;
  final StatutPresence statut;
  final String? motif;
  final bool justifie;

  Map<String, dynamic> versJson() {
    final nonPresent = statut != StatutPresence.present;
    final motifNettoye = motif?.trim();
    return {
      'inscriptionId': inscriptionId,
      'statut': statut.code,
      if (nonPresent && motifNettoye != null && motifNettoye.isNotEmpty) 'motif': motifNettoye,
      'justifie': nonPresent && justifie,
    };
  }
}

/// Une évaluation (devoir, composition…) d'une classe.
class EvaluationClasse {
  const EvaluationClasse({
    required this.id,
    required this.titre,
    required this.matiere,
    required this.periode,
    required this.date,
    required this.coefficient,
    required this.bareme,
  });

  final int id;
  final String titre;
  final String matiere;
  final String periode;

  /// AAAA-MM-JJ.
  final String date;
  final double coefficient;

  /// Note maximale (souvent 20).
  final double bareme;

  factory EvaluationClasse.depuisJson(Map<String, dynamic> json) => EvaluationClasse(
        id: (json['id'] as num).toInt(),
        titre: json['titre'] as String? ?? '',
        matiere: json['matiere'] as String? ?? '',
        periode: json['periode'] as String? ?? '',
        date: json['dateEvaluation'] as String? ?? '',
        coefficient: (json['coefficient'] as num?)?.toDouble() ?? 1,
        bareme: (json['bareme'] as num?)?.toDouble() ?? 20,
      );
}

/// La note d'un élève pour une évaluation.
class LigneNote {
  const LigneNote({
    required this.inscriptionId,
    required this.nom,
    required this.prenom,
    required this.matricule,
    this.valeur,
    this.absent = false,
  });

  final int inscriptionId;
  final String nom;
  final String prenom;
  final String matricule;
  final double? valeur;
  final bool absent;

  String get nomComplet => '$nom $prenom';

  factory LigneNote.depuisJson(Map<String, dynamic> json) {
    final eleve = json['eleve'] as Map<String, dynamic>;
    return LigneNote(
      inscriptionId: (json['inscriptionId'] as num).toInt(),
      nom: eleve['nom'] as String,
      prenom: eleve['prenom'] as String,
      matricule: eleve['matricule'] as String,
      valeur: (json['valeur'] as num?)?.toDouble(),
      absent: json['absent'] as bool? ?? false,
    );
  }
}

class GrilleNotes {
  const GrilleNotes({required this.evaluation, required this.lignes});

  final EvaluationClasse evaluation;
  final List<LigneNote> lignes;

  factory GrilleNotes.depuisJson(Map<String, dynamic> json) => GrilleNotes(
        evaluation: EvaluationClasse.depuisJson(json['evaluation'] as Map<String, dynamic>),
        lignes: (json['lignes'] as List<dynamic>)
            .whereType<Map<String, dynamic>>()
            .map(LigneNote.depuisJson)
            .toList(),
      );
}

/// Ligne envoyée au serveur : une valeur, « absent », ou rien (efface la note).
class SaisieNote {
  const SaisieNote({required this.inscriptionId, this.valeur, this.absent = false});

  final int inscriptionId;
  final double? valeur;
  final bool absent;

  Map<String, dynamic> versJson() => {
        'inscriptionId': inscriptionId,
        if (valeur != null) 'valeur': valeur,
        'absent': absent,
      };
}

/// Lit une note saisie ("14,5", "14.5", "") ; null si vide. Lève [FormatException] si invalide.
double? lireNote(String texte, double bareme) {
  final nettoye = texte.trim().replaceAll(',', '.');
  if (nettoye.isEmpty) return null;
  final valeur = double.tryParse(nettoye);
  if (valeur == null) throw const FormatException('Note invalide.');
  if (valeur < 0 || valeur > bareme) throw FormatException('La note doit être comprise entre 0 et ${bareme.toStringAsFixed(0)}.');
  return (valeur * 100).round() / 100;
}
