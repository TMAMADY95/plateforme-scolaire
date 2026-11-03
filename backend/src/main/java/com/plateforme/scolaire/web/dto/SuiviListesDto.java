package com.plateforme.scolaire.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Suivi des listes de candidats d'un examen sur le périmètre de l'utilisateur. enAttenteDe : VERIFICATION (DPE/DCE ou IRE),
 * VALIDATION (DGECS) ou null quand rien n'est attendu.
 */
public record SuiviListesDto(Long examenId, int brouillons, int transmises, int verifiees, int validees,
        List<Ligne> lignes) {

    public record Ligne(Long etablissementId, String etablissement, String zone, int candidats, String statut,
            Instant depuis, Long joursAttente, String enAttenteDe) {}
}
