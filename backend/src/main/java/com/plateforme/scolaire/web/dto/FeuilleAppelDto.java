package com.plateforme.scolaire.web.dto;

import java.time.LocalDate;
import java.util.List;

public record FeuilleAppelDto(Long classeId, LocalDate date, List<LigneFeuilleDto> lignes) {
}
