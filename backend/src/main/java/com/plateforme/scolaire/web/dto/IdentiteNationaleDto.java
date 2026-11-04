package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Eleve;
import java.time.LocalDate;

/** Identité nationale d'un élève : INE et pièce d'état civil. */
public record IdentiteNationaleDto(Long id, String ine, String matricule, String nom, String prenom,
                                   LocalDate dateNaissance, String lieuNaissance,
                                   String typeActeNaissance, String numeroActeNaissance, boolean sansActe) {

    public static IdentiteNationaleDto de(Eleve e) {
        boolean sansActe = e.getTypeActeNaissance() == null || "AUCUN".equals(e.getTypeActeNaissance());
        return new IdentiteNationaleDto(e.getId(), e.getIne(), e.getMatricule(), e.getNom(), e.getPrenom(),
                e.getDateNaissance(), e.getLieuNaissance(), e.getTypeActeNaissance(), e.getNumeroActeNaissance(), sansActe);
    }
}
