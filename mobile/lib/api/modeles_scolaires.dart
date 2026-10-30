// Données scolaires affichées aux parents et aux élèves.

List<T> _liste<T>(Object? json, T Function(Map<String, dynamic>) depuisJson) =>
    (json as List<dynamic>? ?? const []).whereType<Map<String, dynamic>>().map(depuisJson).toList();

double? _decimal(Object? valeur) => (valeur as num?)?.toDouble();

/// Un enfant rattaché à un parent, ou le dossier de l'élève connecté.
class Enfant {
  const Enfant({
    required this.eleveId,
    required this.matricule,
    required this.nom,
    required this.prenom,
    required this.dateNaissance,
    this.relation = '',
    this.classeId,
    this.classeNom,
    this.etablissementNom,
    this.anneeScolaireId,
    this.anneeLibelle,
    this.estMoi = false,
  });

  final int eleveId;
  final String matricule;
  final String nom;
  final String prenom;
  final String dateNaissance;
  final String relation;
  final int? classeId;
  final String? classeNom;
  final String? etablissementNom;
  final int? anneeScolaireId;
  final String? anneeLibelle;

  /// Vrai quand c'est le dossier de l'élève connecté (ses factures passent par /api/me/factures).
  final bool estMoi;

  /// Réponse de GET /api/me/enfants et de GET /api/me/eleve (le second n'a pas de relation).
  factory Enfant.depuisJson(Map<String, dynamic> json, {bool estMoi = false}) => Enfant(
        eleveId: (json['eleveId'] as num).toInt(),
        matricule: json['matricule'] as String,
        nom: json['nom'] as String,
        prenom: json['prenom'] as String,
        dateNaissance: json['dateNaissance'] as String,
        relation: json['relation'] as String? ?? '',
        classeId: (json['classeId'] as num?)?.toInt(),
        classeNom: json['classeNom'] as String?,
        etablissementNom: json['etablissementNom'] as String?,
        anneeScolaireId: (json['anneeScolaireId'] as num?)?.toInt(),
        anneeLibelle: json['anneeLibelle'] as String?,
        estMoi: estMoi,
      );

  String get nomComplet => '$prenom $nom';

  /// Sans inscription active, il n'y a ni bulletin ni emploi du temps.
  bool get estInscrit => classeId != null && anneeScolaireId != null;

  String get descriptionClasse => classeNom != null
      ? 'Classe $classeNom — $etablissementNom ($anneeLibelle)'
      : "Pas d'inscription active pour le moment";
}

class Periode {
  const Periode({required this.id, required this.libelle, required this.dateDebut, required this.dateFin});

  final int id;
  final String libelle;
  final String dateDebut;
  final String dateFin;

  factory Periode.depuisJson(Map<String, dynamic> json) => Periode(
        id: (json['id'] as num).toInt(),
        libelle: json['libelle'] as String,
        dateDebut: json['dateDebut'] as String,
        dateFin: json['dateFin'] as String,
      );

  static List<Periode> listeDepuisJson(Object? json) => _liste(json, Periode.depuisJson);
}

class LigneBulletin {
  const LigneBulletin({required this.matiere, this.moyenneEleve, this.moyenneClasse, this.coefficient = 1});

  final String matiere;
  final double? moyenneEleve;
  final double? moyenneClasse;

  /// Poids de la matière dans la moyenne générale (1 par défaut).
  final double coefficient;

  factory LigneBulletin.depuisJson(Map<String, dynamic> json) => LigneBulletin(
        matiere: json['matiere'] as String,
        moyenneEleve: _decimal(json['moyenneEleve']),
        moyenneClasse: _decimal(json['moyenneClasse']),
        coefficient: _decimal(json['coefficient']) ?? 1,
      );

  /// Moyenne de l'élève moins moyenne de la classe ; null si l'une des deux manque.
  double? get ecart => (moyenneEleve == null || moyenneClasse == null) ? null : moyenneEleve! - moyenneClasse!;
}

class Bulletin {
  const Bulletin({
    required this.periode,
    required this.matieres,
    required this.effectif,
    this.moyenneGenerale,
    this.moyenneGeneraleClasse,
    this.rang,
  });

  final String periode;
  final List<LigneBulletin> matieres;
  final double? moyenneGenerale;
  final double? moyenneGeneraleClasse;
  final int? rang;
  final int effectif;

  factory Bulletin.depuisJson(Map<String, dynamic> json) => Bulletin(
        periode: json['periode'] as String,
        matieres: _liste(json['matieres'], LigneBulletin.depuisJson),
        moyenneGenerale: _decimal(json['moyenneGenerale']),
        moyenneGeneraleClasse: _decimal(json['moyenneGeneraleClasse']),
        rang: (json['rang'] as num?)?.toInt(),
        effectif: (json['effectif'] as num?)?.toInt() ?? 0,
      );
}

enum StatutPresence {
  present('PRESENT', 'Présent'),
  absent('ABSENT', 'Absent'),
  retard('RETARD', 'Retard');

  const StatutPresence(this.code, this.libelle);

  final String code;
  final String libelle;

  /// Un statut inconnu est traité comme une présence plutôt que de faire échouer l'écran.
  static StatutPresence depuisCode(String? code) =>
      StatutPresence.values.firstWhere((s) => s.code == code, orElse: () => StatutPresence.present);
}

class PresenceEleve {
  const PresenceEleve({required this.id, required this.date, required this.statut, this.motif, this.justifie = false});

  final int id;
  final String date;
  final StatutPresence statut;
  final String? motif;
  final bool justifie;

  factory PresenceEleve.depuisJson(Map<String, dynamic> json) => PresenceEleve(
        id: (json['id'] as num).toInt(),
        date: json['date'] as String,
        statut: StatutPresence.depuisCode(json['statut'] as String?),
        motif: json['motif'] as String?,
        justifie: json['justifie'] as bool? ?? false,
      );

  static List<PresenceEleve> listeDepuisJson(Object? json) => _liste(json, PresenceEleve.depuisJson);
}

/// Une annonce à toute une classe.
class Annonce {
  const Annonce({
    required this.id,
    required this.auteur,
    required this.titre,
    required this.contenu,
    required this.publieLe,
    this.modifiable = false,
  });

  final int id;
  final String auteur;
  final String titre;
  final String contenu;

  /// Horodatage ISO 8601 (UTC).
  final String publieLe;

  /// L'utilisateur peut supprimer cette annonce (auteur ou direction).
  final bool modifiable;

  factory Annonce.depuisJson(Map<String, dynamic> json) => Annonce(
        id: (json['id'] as num).toInt(),
        auteur: json['auteur'] as String? ?? '',
        titre: json['titre'] as String? ?? '',
        contenu: json['contenu'] as String? ?? '',
        publieLe: json['publieLe'] as String? ?? '',
        modifiable: json['modifiable'] as bool? ?? false,
      );

  static List<Annonce> listeDepuisJson(Object? json) => _liste(json, Annonce.depuisJson);
}

/// Un devoir à faire pour une classe.
class Devoir {
  const Devoir({
    required this.id,
    required this.matiere,
    required this.enseignant,
    required this.titre,
    required this.dateLimite,
    this.consigne,
    this.fait,
    this.faits,
    this.effectif,
    this.matiereId,
  });

  final int id;
  final int? matiereId;
  final String matiere;
  final String enseignant;
  final String titre;
  final String? consigne;

  /// Au format AAAA-MM-JJ.
  final String dateLimite;

  /// Renseigné dans la liste d'un élève.
  final bool? fait;

  /// Renseignés dans la liste de l'équipe : élèves ayant terminé / élèves de la classe.
  final int? faits;
  final int? effectif;

  factory Devoir.depuisJson(Map<String, dynamic> json) => Devoir(
        id: (json['id'] as num).toInt(),
        matiereId: (json['matiereId'] as num?)?.toInt(),
        matiere: json['matiere'] as String? ?? '',
        enseignant: json['enseignant'] as String? ?? '',
        titre: json['titre'] as String,
        consigne: json['consigne'] as String?,
        dateLimite: json['dateLimite'] as String,
        fait: json['fait'] as bool?,
        faits: (json['faits'] as num?)?.toInt(),
        effectif: (json['effectif'] as num?)?.toInt(),
      );

  static List<Devoir> listeDepuisJson(Object? json) => _liste(json, Devoir.depuisJson);
}

class CreneauEleve {
  const CreneauEleve({
    required this.matiere,
    required this.jourSemaine,
    required this.heureDebut,
    required this.heureFin,
    this.enseignantNom,
    this.salleNom,
  });

  final String matiere;
  final String? enseignantNom;
  final String? salleNom;

  /// 1 = lundi ... 7 = dimanche.
  final int jourSemaine;
  final String heureDebut;
  final String heureFin;

  factory CreneauEleve.depuisJson(Map<String, dynamic> json) => CreneauEleve(
        matiere: json['matiereLibelle'] as String,
        enseignantNom: json['enseignantNom'] as String?,
        salleNom: json['salleNom'] as String?,
        jourSemaine: (json['jourSemaine'] as num).toInt(),
        heureDebut: json['heureDebut'] as String,
        heureFin: json['heureFin'] as String,
      );

  static List<CreneauEleve> listeDepuisJson(Object? json) => _liste(json, CreneauEleve.depuisJson);
}

List<Enfant> enfantsDepuisJson(Object? json) => _liste(json, Enfant.depuisJson);

enum StatutFacture {
  impayee('IMPAYEE', 'Impayée'),
  partielle('PARTIELLE', 'Partielle'),
  payee('PAYEE', 'Payée'),
  exoneree('EXONEREE', 'Exonérée'),
  annulee('ANNULEE', 'Annulée');

  const StatutFacture(this.code, this.libelle);

  final String code;
  final String libelle;

  static StatutFacture depuisCode(String? code) =>
      StatutFacture.values.firstWhere((s) => s.code == code, orElse: () => StatutFacture.impayee);
}

class Facture {
  const Facture({
    required this.id,
    required this.numero,
    required this.libelle,
    required this.montantDu,
    required this.montantPaye,
    required this.resteAPayer,
    required this.devise,
    required this.statut,
    required this.enRetard,
    this.echeance,
  });

  final int id;
  final String numero;
  final String libelle;
  final double montantDu;
  final double montantPaye;
  final double resteAPayer;
  final String devise;
  final String? echeance;
  final StatutFacture statut;
  final bool enRetard;

  factory Facture.depuisJson(Map<String, dynamic> json) => Facture(
        id: (json['id'] as num).toInt(),
        numero: json['numero'] as String,
        libelle: json['libelle'] as String,
        montantDu: (json['montantDu'] as num).toDouble(),
        montantPaye: (json['montantPaye'] as num).toDouble(),
        resteAPayer: (json['resteAPayer'] as num).toDouble(),
        devise: json['devise'] as String? ?? '',
        echeance: json['echeance'] as String?,
        statut: StatutFacture.depuisCode(json['statut'] as String?),
        enRetard: json['enRetard'] as bool? ?? false,
      );

  static List<Facture> listeDepuisJson(Object? json) => _liste(json, Facture.depuisJson);

  /// Une facture se paie en ligne tant qu'elle est impayée ou partielle avec un reste.
  bool get payable =>
      (statut == StatutFacture.impayee || statut == StatutFacture.partielle) && resteAPayer > 0;
}

/// Réponse de l'initiation d'un paiement en ligne : l'adresse où le terminer.
class PaiementInitie {
  const PaiementInitie({required this.reference, required this.urlPaiement});

  final String reference;
  final String urlPaiement;

  factory PaiementInitie.depuisJson(Map<String, dynamic> json) => PaiementInitie(
        reference: json['reference'] as String,
        urlPaiement: json['urlPaiement'] as String,
      );
}
