package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InscriptionCandidatsRequest(@NotEmpty @Size(max = 500) List<Long> eleveIds) {
}
