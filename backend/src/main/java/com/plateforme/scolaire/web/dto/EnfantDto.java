package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.LienParental;
import java.time.LocalDate;

/** Un enfant rattaché au parent connecté, avec sa classe actuelle (inscription acceptée la plus récente). */
public record EnfantDto(Long eleveId, String matricule, String nom, String prenom, LocalDate dateNaissance,
                        String sexe, String relation, boolean responsableLegal, Long classeId, String classeNom,
                        Long etablissementId, String etablissementNom, Long anneeScolaireId, String anneeLibelle) {

    public static EnfantDto de(LienParental lien, Inscription inscriptionActive) {
        var eleve = lien.getEleve();
        boolean inscrit = inscriptionActive != null && inscriptionActive.getClasse() != null;
        return new EnfantDto(
                eleve.getId(), eleve.getMatricule(), eleve.getNom(), eleve.getPrenom(), eleve.getDateNaissance(),
                eleve.getSexe(), lien.getRelation(), lien.isResponsableLegal(),
                inscrit ? inscriptionActive.getClasse().getId() : null,
                inscrit ? inscriptionActive.getClasse().getNom() : null,
                inscrit ? inscriptionActive.getEtablissement().getId() : null,
                inscrit ? inscriptionActive.getEtablissement().getNom() : null,
                inscrit ? inscriptionActive.getAnneeScolaire().getId() : null,
                inscrit ? inscriptionActive.getAnneeScolaire().getLibelle() : null);
    }
}
