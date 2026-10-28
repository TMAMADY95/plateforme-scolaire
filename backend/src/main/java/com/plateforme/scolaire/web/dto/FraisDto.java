package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Frais;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Une ligne de la grille tarifaire d'un établissement. */
public record FraisDto(Long id, Long etablissementId, Long anneeScolaireId, Long niveauId,
                       String niveauLibelle, String libelle, BigDecimal montant, String devise,
                       LocalDate echeance, boolean obligatoire, boolean actif) {

    public static FraisDto de(Frais f) {
        return new FraisDto(
                f.getId(),
                f.getEtablissement().getId(),
                f.getAnneeScolaire().getId(),
                f.getNiveau() == null ? null : f.getNiveau().getId(),
                f.getNiveau() == null ? null : f.getNiveau().getLibelle(),
                f.getLibelle(), f.getMontant(), f.getDevise(), f.getEcheance(),
                f.isObligatoire(), f.isActif());
    }
}
