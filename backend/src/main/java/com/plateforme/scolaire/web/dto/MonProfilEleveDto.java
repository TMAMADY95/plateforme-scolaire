package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.domain.Inscription;
import java.time.LocalDate;

/** Profil de l'élève connecté, avec sa classe actuelle (inscription acceptée la plus récente). */
public record MonProfilEleveDto(Long eleveId, String matricule, String nom, String prenom,
                                LocalDate dateNaissance, String sexe, Long classeId, String classeNom,
                                Long etablissementId, String etablissementNom, Long anneeScolaireId,
                                String anneeLibelle) {

    public static MonProfilEleveDto de(Eleve eleve, Inscription inscriptionActive) {
        boolean inscrit = inscriptionActive != null && inscriptionActive.getClasse() != null;
        return new MonProfilEleveDto(
                eleve.getId(), eleve.getMatricule(), eleve.getNom(), eleve.getPrenom(),
                eleve.getDateNaissance(), eleve.getSexe(),
                inscrit ? inscriptionActive.getClasse().getId() : null,
                inscrit ? inscriptionActive.getClasse().getNom() : null,
                inscrit ? inscriptionActive.getEtablissement().getId() : null,
                inscrit ? inscriptionActive.getEtablissement().getNom() : null,
                inscrit ? inscriptionActive.getAnneeScolaire().getId() : null,
                inscrit ? inscriptionActive.getAnneeScolaire().getLibelle() : null);
    }
}
