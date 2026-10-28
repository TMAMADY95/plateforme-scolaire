package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Etablissement;

public record EtablissementDto(Long id, String codeOfficiel, String nom, String typeEtablissement,
                               String statut, Long zoneId, String adresse, Integer capacite) {

    public static EtablissementDto de(Etablissement e) {
        return new EtablissementDto(
                e.getId(),
                e.getCodeOfficiel(),
                e.getNom(),
                e.getTypeEtablissement(),
                e.getStatut(),
                e.getZone() != null ? e.getZone().getId() : null,
                e.getAdresse(),
                e.getCapacite());
    }
}
