package com.plateforme.scolaire.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Les élèves absents de la liste ne sont pas modifiés. */
public record NotesRequest(@NotEmpty @Size(max = 300) List<@Valid LigneNoteRequest> lignes) {
}
