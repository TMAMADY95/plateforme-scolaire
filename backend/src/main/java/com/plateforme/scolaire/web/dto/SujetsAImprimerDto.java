package com.plateforme.scolaire.web.dto;

import java.util.List;

/**
 * Besoins d'impression d'un examen : exemplaires de sujets, feuilles de composition et enveloppes par épreuve, comparés aux exemplaires
 * déjà conditionnés pour les centres. Les sujets sont des données confidentielles : seuls des effectifs figurent ici, jamais leur contenu.
 */
public record SujetsAImprimerDto(Long examenId, int candidats, int salles, int margePourcent, int feuillesParCandidat, List<Epreuve> epreuves,
        List<Option> options) {

    public record Epreuve(Long epreuveId, String discipline, String date, int candidats, int sujetsAImprimer, int feuilles, int enveloppes,
            int exemplairesConditionnes, int ecart) {
    }

    /** Candidats par série (option) du lycée ; « Sans série » regroupe les autres. */
    public record Option(String option, int candidats, int sujetsAImprimer) {
    }
}
