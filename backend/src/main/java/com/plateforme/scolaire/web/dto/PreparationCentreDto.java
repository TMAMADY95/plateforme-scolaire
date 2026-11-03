package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;

/** Checklist de préparation matérielle d'un centre : les sept travaux, et si le centre est prêt. */
public record PreparationCentreDto(Long centreId, String centre, boolean pret, int faits, int total, List<Element> elements) {

    public record Element(String code, String libelle, String travaux, boolean fait, String observation, Instant faitLe, String faitPar) {
    }
}
