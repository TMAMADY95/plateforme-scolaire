package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Doublons possibles de dossiers élèves (même nom, prénom et date de naissance) et fusion. */
public record FusionEleveDto(List<Groupe> groupes) {

    public record Groupe(String nom, String prenom, LocalDate dateNaissance, List<Dossier> dossiers) {
    }

    public record Dossier(Long id, String matricule, String identifiantNational, String lieuNaissance, String nomMere, int inscriptions, String dernierEtablissement) {
    }

    public record Requete(@NotNull Long doublonId, @NotBlank @Size(max = 500) String motif) {
    }

    public record Resultat(Long conserveId, String conserveIdentifiant, String absorbeIdentifiant, int lignesReprises) {
    }
}
