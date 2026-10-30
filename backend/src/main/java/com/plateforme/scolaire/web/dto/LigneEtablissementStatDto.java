package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Etablissement;
import java.util.List;

/** Les effectifs d'un établissement pour une année : une ligne du tableau « par établissement ». */
public record LigneEtablissementStatDto(Long id, String codeOfficiel, String nom, String secteur,
                                        List<CycleDto> cycles, String statut, Long zoneId, String zone,
                                        long eleves, long enseignants, long personnelDirection, long classes) {

    public static LigneEtablissementStatDto de(Etablissement e, long eleves, long enseignants,
                                               long personnelDirection, long classes) {
        EtablissementDto base = EtablissementDto.de(e);
        return new LigneEtablissementStatDto(e.getId(), e.getCodeOfficiel(), e.getNom(), e.getSecteur(),
                base.cycles(), e.getStatut(), base.zoneId(), e.getZone() == null ? null : e.getZone().getNom(),
                eleves, enseignants, personnelDirection, classes);
    }
}
