package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResultatsClasseDto(Long classeId, Long periodeId, List<MatiereDto> matieres,
                                 List<LigneResultatDto> eleves, Map<Long, BigDecimal> moyennesClasseParMatiere,
                                 BigDecimal moyenneGeneraleClasse) {
}
