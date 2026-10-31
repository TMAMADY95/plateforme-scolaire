package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Banque de sujets d'un examen : pour chaque épreuve, ses variantes et l'état du tirage. */
public record BanqueSujetsDto(Long examenId, List<EpreuveSujets> epreuves) {

    public record EpreuveSujets(Long id, String nom, LocalDate date, List<Sujet> variantes, int validees, boolean tirable,
                                String varianteTiree) {
    }

    public record Sujet(Long id, String variante, String intitule, String auteurs, String statut, Instant valideLe, Instant tireLe) {
    }
}
