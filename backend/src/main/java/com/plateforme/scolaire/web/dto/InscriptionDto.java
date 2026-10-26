package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Inscription;
import java.time.LocalDate;

public record InscriptionDto(Long id, Long etablissementId, Long anneeScolaireId, Long classeId, String classeNom,
                             String statut, LocalDate dateDemande, LocalDate dateDecision, EleveDto eleve) {

    public static InscriptionDto de(Inscription i) {
        return new InscriptionDto(
                i.getId(),
                i.getEtablissement().getId(),
                i.getAnneeScolaire().getId(),
                i.getClasse() != null ? i.getClasse().getId() : null,
                i.getClasse() != null ? i.getClasse().getNom() : null,
                i.getStatut(),
                i.getDateDemande(),
                i.getDateDecision(),
                EleveDto.de(i.getEleve()));
    }
}
