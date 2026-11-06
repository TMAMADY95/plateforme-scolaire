package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Manuels scolaires : catalogue, dotation d'un établissement et synthèse sur un périmètre. */
public record ManuelsDto() {

    public record Manuel(Long id, Long matiereId, String matiere, Long niveauId, String niveau, String titre, String editeur, Integer anneeEdition,
            String serie) {
    }

    public record NouveauManuel(@NotNull Long matiereId, @NotNull Long niveauId, @NotBlank @Size(max = 200) String titre, @Size(max = 150) String editeur,
            @Min(1900) @Max(2100) Integer anneeEdition, @Size(max = 30) String serie) {
    }

    /**
     * effectif : élèves du niveau dans l'établissement ; exemplaires : reçus moins perdus ; couverture : exemplaires / effectif en % (null sans élève) ;
     * etat : COMPLET (≥ 100 %), PARTIEL (≥ 50 %), INSUFFISANT, ou SANS_ELEVE.
     */
    public record Ligne(Long manuelId, String matiere, String niveau, String titre, int effectif, int recus, int distribues, int perdus, int disponibles,
            int exemplaires, Double couverture, String etat, String observation) {
    }

    public record Dotation(Long etablissementId, Long anneeId, List<Ligne> lignes) {
    }

    public record Quantites(@Min(0) @Max(1000000) int recus, @Min(0) @Max(1000000) int distribues, @Min(0) @Max(1000000) int perdus, @Size(max = 500) String observation) {
    }

    public record LigneEtablissement(Long etablissementId, String etablissement, String zone, int besoin, int exemplaires, Double couverture, int manuelsInsuffisants,
            int manuelsConcernes) {
    }

    public record Synthese(Long anneeId, int besoin, int exemplaires, Double couverture, int etablissementsInsuffisants, List<LigneEtablissement> etablissements) {
    }
}
