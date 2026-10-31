package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Size;

public record DecisionJustificationRequest(boolean accepter, @Size(max = 500) String reponse) {
}
