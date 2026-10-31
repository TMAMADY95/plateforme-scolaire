package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Jury de délibération d'un examen : membres, quorum (deux tiers des membres) et avancement des résultats. */
public record JuryDto(Long examenId, String statut, LocalDate dateDeliberation, LocalDate dateProclamation,
                      List<Membre> membres, int presents, int quorumRequis, boolean quorumAtteint, boolean peutProclamer) {

    public record Membre(Long id, int ordre, String fonction, String nom, boolean present) {
    }
}
