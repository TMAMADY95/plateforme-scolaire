package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Eleve;
import java.time.LocalDate;

public record EleveDto(Long id, String matricule, String nom, String prenom, LocalDate dateNaissance, String sexe) {

    public static EleveDto de(Eleve e) {
        return new EleveDto(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getDateNaissance(),
                e.getSexe());
    }
}
