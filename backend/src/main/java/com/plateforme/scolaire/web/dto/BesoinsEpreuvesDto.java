package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/** Besoins en personnel par épreuve : pour chaque fonction déclarable, le pic de besoin sur l'examen face au personnel disponible. */
public record BesoinsEpreuvesDto(Long examenId, List<Fonction> fonctions, List<Epreuve> epreuves) {

    /** pic : plus grand besoin déclaré sur une épreuve, null sans besoin ; manque : pic - disponibles, au minimum 0. */
    public record Fonction(String fonction, String libelle, int disponibles, Integer pic, Integer manque) {
    }

    /** requis : besoin déclaré par fonction pour cette épreuve (absent = non déclaré). */
    public record Epreuve(Long epreuveId, String discipline, String date, String heureDebut, Map<String, Integer> requis) {
    }

    public record Requete(@NotBlank @Size(max = 15) String fonction, @NotNull @Min(0) Integer requis) {
    }
}
