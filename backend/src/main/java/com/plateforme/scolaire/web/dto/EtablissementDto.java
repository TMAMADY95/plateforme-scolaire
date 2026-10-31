package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.domain.Etablissement;
import java.util.Comparator;
import java.util.List;

/** [secteur] : PUBLIC ou PRIVE. [cycles] : un ou plusieurs, dans l'ordre (primaire, collège, lycée…). */
public record EtablissementDto(Long id, String codeOfficiel, String nom, String secteur, List<CycleDto> cycles,
                               String statut, Long zoneId, String adresse, Integer capacite,
                               Long typeEtablissementId, String typeEtablissement) {

    public static EtablissementDto de(Etablissement e) {
        return new EtablissementDto(
                e.getId(),
                e.getCodeOfficiel(),
                e.getNom(),
                e.getSecteur(),
                e.getCycles().stream().sorted(Comparator.comparingInt(CycleScolaire::getOrdre)).map(CycleDto::de).toList(),
                e.getStatut(),
                e.getZone() != null ? e.getZone().getId() : null,
                e.getAdresse(),
                e.getCapacite(),
                e.getTypeEtablissement() == null ? null : e.getTypeEtablissement().getId(),
                e.getTypeEtablissement() == null ? null : e.getTypeEtablissement().getLibelle());
    }
}
