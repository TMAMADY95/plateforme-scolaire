package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Utilisateur;

/** Un compte vu par son administrateur (jamais le mot de passe). */
public record CompteDto(Long id, String email, String nom, String prenom, String role, boolean actif,
                        boolean doitChangerMotDePasse) {

    public static CompteDto de(Utilisateur u, String role) {
        return new CompteDto(u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), role, u.isActif(),
                u.isDoitChangerMotDePasse());
    }
}
