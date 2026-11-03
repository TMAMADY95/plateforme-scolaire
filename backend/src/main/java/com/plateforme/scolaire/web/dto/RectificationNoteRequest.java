package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Acceptation d'une réclamation sur une note : l'épreuve concernée, la note corrigée et la motivation. */
public record RectificationNoteRequest(@NotNull Long epreuveId, @NotNull @DecimalMin("0") BigDecimal nouvelleNote, @NotBlank @Size(max = 1000) String decision) {
}
