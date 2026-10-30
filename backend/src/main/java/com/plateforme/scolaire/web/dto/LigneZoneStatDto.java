package com.plateforme.scolaire.web.dto;

/** Les effectifs d'une zone (région, département…), sous-zones comprises : une ligne du tableau « par zone ». */
public record LigneZoneStatDto(Long zoneId, String nom, String typeZone, Long parentId, long etablissements,
                               long eleves, long enseignants, long personnelDirection, long classes) {
}
