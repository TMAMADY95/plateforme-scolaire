package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Un établissement et son éligibilité comme centre d'examen (motifs de refus s'il ne l'est pas). */
public record EtablissementCentreDto(Long id, String codeOfficiel, String nom, Long zoneId, String zone, String secteur,
                                     String typeEtablissement, long salles, long capacite, boolean eligible,
                                     List<String> motifs) {
}
