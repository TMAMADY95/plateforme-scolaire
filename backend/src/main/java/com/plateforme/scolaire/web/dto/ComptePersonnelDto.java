package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Utilisateur;

/** Un compte créé par le ministère (jamais le mot de passe). */
public record ComptePersonnelDto(Long id, String email, String nom, String prenom, String role) {

    public static ComptePersonnelDto de(Utilisateur u, String role) {
        return new ComptePersonnelDto(u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), role);
    }
}
