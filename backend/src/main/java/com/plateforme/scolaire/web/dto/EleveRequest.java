package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Lieu de naissance, père et mère sont facultatifs : ils figurent sur les diplômes d'examen. */
public record EleveRequest(@NotBlank @Size(max = 100) String nom,
                           @NotBlank @Size(max = 100) String prenom,
                           @NotNull @Past LocalDate dateNaissance,
                           @Pattern(regexp = "[FM]", message = "doit valoir F ou M") String sexe,
                           @Size(max = 150) String lieuNaissance,
                           @Size(max = 150) String nomPere,
                           @Size(max = 150) String nomMere) {

    public EleveRequest(String nom, String prenom, LocalDate dateNaissance, String sexe) {
        this(nom, prenom, dateNaissance, sexe, null, null, null);
    }

    static String propre(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public String lieuNaissancePropre() {
        return propre(lieuNaissance);
    }

    public String nomPerePropre() {
        return propre(nomPere);
    }

    public String nomMerePropre() {
        return propre(nomMere);
    }
}
