package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Personnel mobilisé pour un examen, par fonction : requis (calculé ou déclaré), disponible et manque. */
public record PersonnelNationalDto(Long examenId, List<Ligne> fonctions, List<Personne> personnes) {

    /** requis est null tant qu'aucun besoin n'est déclaré ; source : CENTRES, COMMISSIONS ou NATIONAL. */
    public record Ligne(String fonction, String libelle, Integer requis, int disponibles, Integer manque, boolean besoinModifiable, boolean personnesSaisies,
            String source) {
    }

    public record Personne(Long id, String fonction, String nom, String telephone) {
    }

    public record AjoutRequest(@NotBlank @Size(max = 15) String fonction, @NotBlank @Size(max = 150) String nom, @Size(max = 30) String telephone) {
    }

    public record BesoinRequest(@NotBlank @Size(max = 15) String fonction, @NotNull @Min(0) Integer requis) {
    }
}
