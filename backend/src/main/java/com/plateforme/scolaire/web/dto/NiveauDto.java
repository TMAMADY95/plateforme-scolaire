package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Niveau;

/** [cycleId] et [cycle] (libellé) sont nuls tant que le niveau n'est rattaché à aucun cycle. */
public record NiveauDto(Long id, String code, String libelle, int ordre, Long cycleId, String cycle) {

    public static NiveauDto de(Niveau n) {
        return new NiveauDto(n.getId(), n.getCode(), n.getLibelle(), n.getOrdre(),
                n.getCycle() == null ? null : n.getCycle().getId(),
                n.getCycle() == null ? null : n.getCycle().getLibelle());
    }
}
