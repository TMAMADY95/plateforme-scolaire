package com.plateforme.scolaire.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Besoins en enseignants. « Vacants » = postes prévus - enseignants en poste. « Besoin estimé » = enseignants
 * nécessaires pour respecter la norme d'élèves par enseignant, moins ceux déjà en poste.
 */
public record RapportPostesDto(int normeElevesParEnseignant, Totaux totaux, List<Ligne> lignes) {

    public record Totaux(long etablissements, long eleves, long enseignants, long postes, long vacants,
                         long besoinEstime, long enSurnombre) {
    }

    public record Ligne(Long id, String codeOfficiel, String nom, String secteur, Long zoneId, String zone,
                        long eleves, long enseignants, Integer postes, Integer vacants, int enSurnombre,
                        int besoinEstime, BigDecimal elevesParEnseignant) {
    }
}
