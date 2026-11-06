package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Eleve;
import java.time.LocalDate;

public record EleveDto(Long id, String matricule, String nom, String prenom, LocalDate dateNaissance, String sexe,
                       String lieuNaissance, String nomPere, String nomMere, String identifiantNational) {

    public EleveDto(Long id, String matricule, String nom, String prenom, LocalDate dateNaissance, String sexe,
                    String lieuNaissance, String nomPere, String nomMere) {
        this(id, matricule, nom, prenom, dateNaissance, sexe, lieuNaissance, nomPere, nomMere, null);
    }

    public EleveDto(Long id, String matricule, String nom, String prenom, LocalDate dateNaissance, String sexe) {
        this(id, matricule, nom, prenom, dateNaissance, sexe, null, null, null);
    }

    public static EleveDto de(Eleve e) {
        return new EleveDto(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getDateNaissance(),
                e.getSexe(), e.getLieuNaissance(), e.getNomPere(), e.getNomMere(), e.getIdentifiantNational());
    }
}
