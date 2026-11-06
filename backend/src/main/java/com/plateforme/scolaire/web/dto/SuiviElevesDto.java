package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Abandon scolaire et situation de vulnérabilité des élèves : suivi nominatif (direction) et synthèse (totaux). */
public record SuiviElevesDto() {

    public record Abandon(Long id, LocalDate dateAbandon, String motif, String observation) {
    }

    public record Ligne(Long inscriptionId, Long eleveId, String matricule, String nom, String classe, String sexe, String orphelin, String handicap,
            Abandon abandon) {
    }

    public record Suivi(Long etablissementId, Long anneeId, List<Ligne> eleves) {
    }

    public record DeclarationAbandon(@NotNull Long inscriptionId, @NotNull LocalDate dateAbandon,
            @NotNull @Pattern(regexp = "MALADIE|MARIAGE_PRECOCE|GROSSESSE|TRAVAIL|DISTANCE|DIFFICULTES_FINANCIERES|DEMENAGEMENT|DECES|ECHEC_SCOLAIRE|AUTRE") String motif,
            @Size(max = 500) String observation) {
    }

    public record Vulnerabilite(@NotNull @Pattern(regexp = "AUCUN|PERE|MERE|DEUX") String orphelin,
            @Pattern(regexp = "MOTEUR|VISUEL|AUDITIF|INTELLECTUEL|AUTRE") String handicap) {
    }

    /** tauxAbandon : abandons pour 100 élèves inscrits, une décimale ; null sans élève. */
    public record LigneEtablissement(Long etablissementId, String etablissement, String zone, int eleves, int abandons, int fillesAbandons, Double tauxAbandon,
            int orphelins, int handicapes) {
    }

    public record Motif(String motif, int nombre) {
    }

    public record Resume(int eleves, int abandons, int fillesAbandons, Double tauxAbandon, int orphelins, int handicapes) {
    }

    public record Synthese(Long anneeId, Resume resume, List<Motif> motifs, List<LigneEtablissement> etablissements) {
    }
}
