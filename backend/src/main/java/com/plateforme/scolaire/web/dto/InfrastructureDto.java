package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Infrastructures et équipements d'un établissement, avec les indicateurs calculés face aux effectifs. */
public record InfrastructureDto(Long etablissementId, Long anneeId, boolean renseignee, Effectifs effectifs, Requete fiche, Indicateurs indicateurs,
        Instant misAJourLe, String misAJourPar) {

    public record Effectifs(int total, int filles, int garcons) {
    }

    /** Ratios nuls quand le dénominateur est nul (aucune salle, aucune place…) ; alertes : constats en clair. */
    public record Indicateurs(Double elevesParSalle, Double elevesParPlace, Double fillesParLatrine, Double garconsParLatrine, List<String> alertes) {
    }

    public record Requete(@Min(0) @Max(500) int sallesDur, @Min(0) @Max(500) int sallesProvisoires, @Min(0) @Max(500) int sallesARehabiliter,
            @Min(0) @Max(20000) int placesAssises, @Min(0) @Max(500) int latrinesFilles, @Min(0) @Max(500) int latrinesGarcons,
            @Min(0) @Max(500) int latrinesPersonnel, @NotBlank @Size(max = 10) String eau, @NotBlank @Size(max = 10) String electricite,
            boolean cloture, boolean lavageMains, boolean bibliotheque, boolean laboratoire, boolean salleInformatique, boolean cantine,
            boolean terrainSport, @Size(max = 500) String observation) {
    }

    /** Synthèse sur le périmètre de l'utilisateur. */
    public record Synthese(Long anneeId, Resume resume, List<Ligne> lignes) {
    }

    public record Resume(int etablissements, int renseignes, int avecAlertes, int salles, int sallesProvisoires, int sallesARehabiliter, Double elevesParSalle,
            int sansEau, int sansElectricite, int sansLatrinesFilles) {
    }

    public record Ligne(Long etablissementId, String etablissement, String zone, int eleves, boolean renseignee, int salles, Double elevesParSalle,
            Double elevesParPlace, String eau, String electricite, List<String> alertes) {
    }
}
