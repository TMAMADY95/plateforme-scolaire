package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Enseignant;

public record EnseignantDto(Long id, Long utilisateurId, String email, String nom, String prenom, String matricule) {

    public static EnseignantDto de(Enseignant e) {
        return new EnseignantDto(e.getId(), e.getUtilisateur().getId(), e.getUtilisateur().getEmail(),
                e.getUtilisateur().getNom(), e.getUtilisateur().getPrenom(), e.getMatricule());
    }
}
