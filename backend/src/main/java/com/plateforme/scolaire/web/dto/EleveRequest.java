package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EleveRequest(@NotBlank @Size(max = 100) String nom,
                           @NotBlank @Size(max = 100) String prenom,
                           @NotNull @Past LocalDate dateNaissance,
                           @Pattern(regexp = "[FM]", message = "doit valoir F ou M") String sexe) {
}
