package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Une page du journal d'audit, de la plus récente à la plus ancienne entrée. */
public record PageAuditDto(List<JournalAuditDto> contenu, long total, int page, int taille) {
}
