package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** matiereId n'est pas modifiable après la création (il est ignoré à la modification). */
public record DevoirRequest(Long matiereId,
                            @NotBlank @Size(max = 200) String titre,
                            @Size(max = 4000) String consigne,
                            @NotNull LocalDate dateLimite) {
}
