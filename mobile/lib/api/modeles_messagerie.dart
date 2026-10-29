// Données de la messagerie parent ↔ personnel.

/// Une personne que l'on peut contacter à propos d'un élève.
class Contact {
  const Contact({required this.utilisateurId, required this.nom, required this.role});

  final int utilisateurId;
  final String nom;
  final String role;

  factory Contact.depuisJson(Map<String, dynamic> json) => Contact(
        utilisateurId: (json['utilisateurId'] as num).toInt(),
        nom: json['nom'] as String? ?? '',
        role: json['role'] as String? ?? '',
      );
}

class MessageFil {
  const MessageFil({required this.id, required this.auteur, required this.moi, required this.contenu, required this.envoyeLe});

  final int id;
  final String auteur;
  final bool moi;
  final String contenu;

  /// Horodatage ISO 8601 (UTC).
  final String envoyeLe;

  factory MessageFil.depuisJson(Map<String, dynamic> json) => MessageFil(
        id: (json['id'] as num).toInt(),
        auteur: json['auteur'] as String? ?? '',
        moi: json['moi'] as bool? ?? false,
        contenu: json['contenu'] as String? ?? '',
        envoyeLe: json['envoyeLe'] as String? ?? '',
      );
}

class Conversation {
  const Conversation({
    required this.id,
    required this.eleve,
    required this.sujet,
    required this.interlocuteur,
    required this.roleInterlocuteur,
    required this.dernierMessageLe,
    required this.apercu,
    required this.nonLu,
    required this.active,
    required this.messages,
  });

  final int id;
  final String eleve;
  final String sujet;
  final String interlocuteur;
  final String roleInterlocuteur;
  final String dernierMessageLe;
  final String apercu;
  final bool nonLu;

  /// Faux quand la relation n'existe plus : la conversation reste lisible, on n'y répond plus.
  final bool active;
  final List<MessageFil> messages;

  factory Conversation.depuisJson(Map<String, dynamic> json) => Conversation(
        id: (json['id'] as num).toInt(),
        eleve: json['eleve'] as String? ?? '',
        sujet: json['sujet'] as String? ?? '',
        interlocuteur: json['interlocuteur'] as String? ?? '',
        roleInterlocuteur: json['roleInterlocuteur'] as String? ?? '',
        dernierMessageLe: json['dernierMessageLe'] as String? ?? '',
        apercu: json['apercu'] as String? ?? '',
        nonLu: json['nonLu'] as bool? ?? false,
        active: json['active'] as bool? ?? false,
        messages: ((json['messages'] as List<dynamic>?) ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(MessageFil.depuisJson)
            .toList(),
      );

  static List<Conversation> listeDepuisJson(Object? json) => json is List
      ? json.whereType<Map<String, dynamic>>().map(Conversation.depuisJson).toList()
      : <Conversation>[];
}
