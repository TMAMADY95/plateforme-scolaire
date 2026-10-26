package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.LienParental;

public record LienParentalDto(Long id, Long eleveId, Long utilisateurId, String email, String nom, String prenom,
                              String relation, boolean responsableLegal) {

    public static LienParentalDto de(LienParental l) {
        return new LienParentalDto(l.getId(), l.getEleve().getId(), l.getUtilisateur().getId(),
                l.getUtilisateur().getEmail(), l.getUtilisateur().getNom(), l.getUtilisateur().getPrenom(),
                l.getRelation(), l.isResponsableLegal());
    }
}
