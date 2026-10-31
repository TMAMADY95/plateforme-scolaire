package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AffectationRole;

/** Un compte du ministère ou d'une direction régionale (jamais le mot de passe). */
public record CompteInstitutionnelDto(Long id, String email, String nom, String prenom, String telephone,
                                      String role, Long zoneId, String zone, boolean actif) {

    public static CompteInstitutionnelDto de(AffectationRole a) {
        var u = a.getUtilisateur();
        return new CompteInstitutionnelDto(u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), u.getTelephone(),
                a.getRole().name(), a.getZone() == null ? null : a.getZone().getId(),
                a.getZone() == null ? null : a.getZone().getNom(), u.isActif());
    }
}
