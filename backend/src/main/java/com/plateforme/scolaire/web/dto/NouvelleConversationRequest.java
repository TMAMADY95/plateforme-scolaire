package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NouvelleConversationRequest(@NotNull Long eleveId,
                                          @NotNull Long destinataireId,
                                          @NotBlank @Size(max = 200) String sujet,
                                          @NotBlank @Size(max = 4000) String contenu) {
}
