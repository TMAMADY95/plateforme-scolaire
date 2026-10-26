package com.plateforme.scolaire.web.dto;

import java.util.List;

public record GrilleNotesDto(EvaluationDto evaluation, List<LigneNoteDto> lignes) {
}
