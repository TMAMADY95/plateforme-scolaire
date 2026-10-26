package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AffectationEnseignant;

public record AffectationEnseignantDto(Long id, Long classeId, Long enseignantId, String enseignantNom,
                                       Long matiereId, String matiereLibelle) {

    public static AffectationEnseignantDto de(AffectationEnseignant a) {
        String nom = a.getEnseignant().getUtilisateur().getPrenom() + " "
                + a.getEnseignant().getUtilisateur().getNom();
        return new AffectationEnseignantDto(a.getId(), a.getClasse().getId(), a.getEnseignant().getId(), nom,
                a.getMatiere().getId(), a.getMatiere().getLibelle());
    }
}
