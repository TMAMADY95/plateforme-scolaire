package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Candidature libre : identité du candidat (F ou M pour le sexe, lieu de naissance, père et mère : facultatifs). serie : obligatoire pour un examen du lycée, interdite sinon. */
public record CandidatLibreRequest(@NotBlank @Size(max = 100) String nom, @NotBlank @Size(max = 100) String prenom,
                                   @NotNull @Past LocalDate dateNaissance, @Pattern(regexp = "[FM]") String sexe,
                                   @Size(max = 150) String lieuNaissance, @Size(max = 150) String nomPere,
                                   @Size(max = 150) String nomMere, @Size(max = 30) String serie) {

    public CandidatLibreRequest(String nom, String prenom, LocalDate dateNaissance, String sexe, String lieuNaissance, String nomPere, String nomMere) {
        this(nom, prenom, dateNaissance, sexe, lieuNaissance, nomPere, nomMere, null);
    }

    public CandidatLibreRequest(String nom, String prenom, LocalDate dateNaissance, String sexe) {
        this(nom, prenom, dateNaissance, sexe, null, null, null, null);
    }

    private static String propre(String s) {
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
