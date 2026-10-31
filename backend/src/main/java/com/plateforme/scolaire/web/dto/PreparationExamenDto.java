package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Ce que l'établissement voit pour un examen : ses candidats et les élèves qu'il peut encore inscrire. */
public record PreparationExamenDto(ExamenDto examen, List<CandidatDto> candidats, List<ElevePossibleDto> possibles) {
}
