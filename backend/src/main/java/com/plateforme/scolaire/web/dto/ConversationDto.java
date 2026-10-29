package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * [interlocuteur] : l'autre personne. [messages] est vide dans les listes. [active] est faux quand la relation
 * (parent de l élève / personnel de sa classe) n existe plus : la conversation reste lisible mais on n y répond plus.
 * Seul le détail renseigne [active] et [messages].
 */
public record ConversationDto(Long id, Long eleveId, String eleve, String sujet, String interlocuteur,
                              String roleInterlocuteur, Instant dernierMessageLe, String apercu, boolean nonLu,
                              boolean active, List<MessageDto> messages) {

    public record MessageDto(Long id, Long auteurId, String auteur, boolean moi, String contenu, Instant envoyeLe) {
    }
}
