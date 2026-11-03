package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Transport des sujets et du matériel vers les centres d'un examen. */
public record ConvoiExamenDto(Long examenId, Resume resume, List<Convoi> convois) {

    public record Resume(int centres, int centresDesservis, int centresSansConvoi, int convois, int livres, int enRoute, int enRetard, int avecIncident,
            int vehicules, int carburantLitres, int escortes, Double tauxLivresATemps) {
    }

    public record Convoi(Long id, Long centreId, String centre, String vehicule, String chauffeur, String itineraire, boolean escorte, Instant departPrevu,
            Instant arriveePrevue, Instant departReel, Instant arriveeReelle, Integer carburantLitres, String statut, String incidentType,
            String incidentDescription, String observation, boolean enRetard) {
    }

    public record Requete(@NotNull Long centreId, @NotBlank @Size(max = 60) String vehicule, @Size(max = 100) String chauffeur, @Size(max = 300) String itineraire,
            boolean escorte, Instant departPrevu, Instant arriveePrevue, Instant departReel, Instant arriveeReelle, @Min(0) Integer carburantLitres,
            @NotBlank @Size(max = 10) String statut, @Size(max = 25) String incidentType, @Size(max = 500) String incidentDescription,
            @Size(max = 500) String observation) {
    }
}
