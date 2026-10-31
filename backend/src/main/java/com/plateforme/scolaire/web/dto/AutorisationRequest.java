package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AutorisationRequest(
        /** EN_ATTENTE, AUTORISE, NON_AUTORISE ou RETIRE */
        @NotBlank String statut,
        @Size(max = 100) String reference,
        LocalDate dateAutorisation,
        @Size(max = 500) String motif) {
}
