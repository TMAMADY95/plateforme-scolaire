package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.Classe;

/** Une classe et une matière enseignée par l'utilisateur connecté. */
public record MonAffectationDto(Long classeId, String classeNom, String niveauLibelle, Long anneeScolaireId,
                                String anneeLibelle, Long etablissementId, Long matiereId, String matiereLibelle) {

    public static MonAffectationDto de(AffectationEnseignant a) {
        Classe classe = a.getClasse();
        return new MonAffectationDto(classe.getId(), classe.getNom(), classe.getNiveau().getLibelle(),
                classe.getAnneeScolaire().getId(), classe.getAnneeScolaire().getLibelle(),
                classe.getEtablissement().getId(), a.getMatiere().getId(), a.getMatiere().getLibelle());
    }
}
