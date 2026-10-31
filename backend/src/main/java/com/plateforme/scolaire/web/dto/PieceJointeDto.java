package com.plateforme.scolaire.web.dto;

import java.time.Instant;

public record PieceJointeDto(Long id, String nom, String typeMime, long taille, Instant ajouteeLe) {
}
