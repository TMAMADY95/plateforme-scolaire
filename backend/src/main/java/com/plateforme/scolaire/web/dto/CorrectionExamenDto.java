package com.plateforme.scolaire.web.dto;

import java.util.List;

/** Commissions de correction d'un examen, avec l'avancement global de la correction. */
public record CorrectionExamenDto(Long examenId, String statutResultats, String niveau, List<EpreuveCitee> epreuves,
                                  List<Commission> commissions, int nbCopies, int nbCopiesCorrigees) {

    public record EpreuveCitee(Long id, String nom, boolean sousCommission) {
    }

    public record Commission(Long id, Long epreuveId, String nom, String lieu, int nbCopies, int nbCopiesCorrigees, boolean cloturee,
                             boolean complete, List<String> manques, List<Membre> membres) {
    }

    public record Membre(Long id, String role, String nom, String telephone) {
    }
}
