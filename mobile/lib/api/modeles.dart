/// Rôles de la plateforme, tels que les renvoie le serveur.
enum Role {
  adminTechnique('ADMIN_TECHNIQUE', 'Administrateur technique'),
  ministere('MINISTERE', 'Ministère'),
  directionRegionale('DIRECTION_REGIONALE', 'Direction régionale'),
  directionEtablissement('DIRECTION_ETABLISSEMENT', "Direction d'établissement"),
  personnelAdmin('PERSONNEL_ADMIN', 'Personnel administratif'),
  enseignant('ENSEIGNANT', 'Enseignant'),
  parent('PARENT', 'Parent'),
  eleve('ELEVE', 'Élève');

  const Role(this.code, this.libelle);

  final String code;
  final String libelle;

  /// Un rôle inconnu (ajouté plus tard côté serveur) renvoie null plutôt que de faire échouer l'application.
  static Role? depuisCode(String? code) {
    for (final role in Role.values) {
      if (role.code == code) return role;
    }
    return null;
  }
}

class Affectation {
  const Affectation({required this.role, this.etablissementId});

  final Role role;
  final int? etablissementId;

  static Affectation? depuisJson(Map<String, dynamic> json) {
    final role = Role.depuisCode(json['role'] as String?);
    if (role == null) return null;
    return Affectation(role: role, etablissementId: (json['etablissementId'] as num?)?.toInt());
  }
}

/// L'utilisateur connecté (réponse de GET /api/me).
class Moi {
  const Moi({
    required this.id,
    required this.email,
    required this.changementMotDePasseRequis,
    required this.affectations,
  });

  final int id;
  final String email;
  final bool changementMotDePasseRequis;
  final List<Affectation> affectations;

  factory Moi.depuisJson(Map<String, dynamic> json) {
    final brutes = (json['affectations'] as List<dynamic>? ?? const []);
    return Moi(
      id: (json['id'] as num).toInt(),
      email: json['email'] as String,
      changementMotDePasseRequis: json['changementMotDePasseRequis'] as bool? ?? false,
      affectations: brutes
          .whereType<Map<String, dynamic>>()
          .map(Affectation.depuisJson)
          .whereType<Affectation>()
          .toList(),
    );
  }

  bool aLeRole(Role role) => affectations.any((a) => a.role == role);

  bool get estParent => aLeRole(Role.parent);
  bool get estEleve => aLeRole(Role.eleve);

  /// Cette application mobile sert les parents et les élèves ; les autres rôles passent par le site web.
  bool get estUtilisateurMobile => estParent || estEleve;

  List<Role> get roles => {for (final a in affectations) a.role}.toList();
}

/// Réponse de POST /api/auth/login.
class ReponseConnexion {
  const ReponseConnexion({required this.jeton, required this.changementMotDePasseRequis});

  final String jeton;
  final bool changementMotDePasseRequis;

  factory ReponseConnexion.depuisJson(Map<String, dynamic> json) => ReponseConnexion(
        jeton: json['token'] as String,
        changementMotDePasseRequis: json['changementMotDePasseRequis'] as bool? ?? false,
      );
}
