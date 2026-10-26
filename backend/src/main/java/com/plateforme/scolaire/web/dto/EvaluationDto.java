package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Evaluation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationDto(Long id, Long classeId, Long matiereId, String matiere, Long periodeId, String periode,
                            Long enseignantId, String titre, LocalDate dateEvaluation, BigDecimal coefficient,
                            BigDecimal bareme) {

    public static EvaluationDto de(Evaluation e) {
        return new EvaluationDto(e.getId(), e.getClasse().getId(), e.getMatiere().getId(), e.getMatiere().getLibelle(),
                e.getPeriode().getId(), e.getPeriode().getLibelle(), e.getEnseignant().getId(), e.getTitre(),
                e.getDateEvaluation(), e.getCoefficient(), e.getBareme());
    }
}
