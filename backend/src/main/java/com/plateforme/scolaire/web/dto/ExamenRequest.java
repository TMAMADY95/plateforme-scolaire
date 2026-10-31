package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** [typesEtablissementIds] absent ou vide = tous les types participent. [niveauOrganisation] absent = NATIONAL. [niveauId] (classe d'examen) donne le cycle quand [cycleId] est absent. */
public record ExamenRequest(@NotBlank @Size(max = 150) String libelle, @NotNull Long anneeScolaireId, Long cycleId,
                            Long niveauId, @Size(max = 50) String niveauOrganisation,
                            java.time.LocalDate dateDebut, java.time.LocalDate dateFin,
                            java.util.List<Long> typesEtablissementIds) {

    public ExamenRequest(String libelle, Long anneeScolaireId, Long cycleId) {
        this(libelle, anneeScolaireId, cycleId, null, null, null, null, null);
    }

    public ExamenRequest(String libelle, Long anneeScolaireId, Long cycleId, Long niveauId, String niveauOrganisation) {
        this(libelle, anneeScolaireId, cycleId, niveauId, niveauOrganisation, null, null, null);
    }
}
