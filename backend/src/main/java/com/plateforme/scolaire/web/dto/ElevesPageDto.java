package com.plateforme.scolaire.web.dto;

import java.util.List;

public record ElevesPageDto(List<EleveListeDto> elements, long total, int page, int taille, int totalPages) {
}
