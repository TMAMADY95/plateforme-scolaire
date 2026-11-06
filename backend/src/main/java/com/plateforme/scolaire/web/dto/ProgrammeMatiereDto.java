package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.ProgrammeMatiere;
import java.math.BigDecimal;

public record ProgrammeMatiereDto(Long id, Long anneeScolaireId, Long niveauId, String niveau, String serie, Long matiereId,
        String matiereCode, String matiere, BigDecimal heuresHebdo, BigDecimal coefficient, String note, Long typeEtablissementId,
        String typeEtablissement, Long cycleId, String cycle) {

    public static ProgrammeMatiereDto de(ProgrammeMatiere p) {
        return new ProgrammeMatiereDto(p.getId(), p.getAnneeScolaire().getId(), p.getNiveau().getId(), p.getNiveau().getLibelle(),
                p.getSerie(), p.getMatiere().getId(), p.getMatiere().getCode(), p.getMatiere().getLibelle(), p.getHeuresHebdo(),
                p.getCoefficient(), p.getNote(), p.getTypeEtablissement() == null ? null : p.getTypeEtablissement().getId(),
                p.getTypeEtablissement() == null ? null : p.getTypeEtablissement().getLibelle(),
                p.getNiveau().getCycle() == null ? null : p.getNiveau().getCycle().getId(),
                p.getNiveau().getCycle() == null ? null : p.getNiveau().getCycle().getLibelle());
    }
}
