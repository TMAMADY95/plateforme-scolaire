package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.ProgrammeMatiere;
import java.math.BigDecimal;

public record ProgrammeMatiereDto(Long id, Long anneeScolaireId, Long niveauId, String niveau, String serie, Long matiereId,
        String matiereCode, String matiere, BigDecimal heuresHebdo, BigDecimal coefficient, String note) {

    public static ProgrammeMatiereDto de(ProgrammeMatiere p) {
        return new ProgrammeMatiereDto(p.getId(), p.getAnneeScolaire().getId(), p.getNiveau().getId(), p.getNiveau().getLibelle(),
                p.getSerie(), p.getMatiere().getId(), p.getMatiere().getCode(), p.getMatiere().getLibelle(), p.getHeuresHebdo(),
                p.getCoefficient(), p.getNote());
    }
}
