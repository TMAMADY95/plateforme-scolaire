package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Discipline d'un établissement : incidents et sanctions. Réservé à la direction ; les parents ne voient que les sanctions de leur enfant. */
public record DisciplineDto(Long etablissementId, Long anneeId, Resume resume, List<Incident> incidents) {

    /** recidivistes : élèves ayant au moins 3 incidents non classés dans l'année. */
    public record Resume(int incidents, int ouverts, int sanctionnes, int classes, int exclusions, int recidivistes) {
    }

    public record Incident(Long id, Long eleveId, String matricule, String eleve, LocalDate dateFaits, String type, String gravite, String description,
            String statut, String sanction, Integer exclusionJours, LocalDate exclusionDebut, String observation, boolean parentsInformes, String signalePar,
            LocalDate signaleLe, String decidePar, LocalDate decideLe, int incidentsAnnee, boolean recidiviste) {
    }

    public record Signalement(@NotBlank @Size(max = 50) String matriculeEleve, @NotNull Long anneeId, @NotNull LocalDate dateFaits, @NotBlank @Size(max = 20) String type,
            @NotBlank @Size(max = 12) String gravite, @NotBlank @Size(max = 1000) String description) {
    }

    public record Sanction(@NotBlank @Size(max = 25) String sanction, @Min(1) @Max(30) Integer exclusionJours, LocalDate exclusionDebut, @Size(max = 500) String observation,
            boolean parentsInformes) {
    }

    public record Classement(@NotBlank @Size(max = 500) String motif) {
    }
}
