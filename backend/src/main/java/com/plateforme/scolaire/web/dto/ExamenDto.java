package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.ExamenNational;

/** [niveauOrganisation] : NATIONAL ou un type de zone (COMMUNE, PREFECTURE, REGION…). */
public record ExamenDto(Long id, String libelle, Long anneeScolaireId, Long cycleId, String cycle,
                        Long niveauId, String niveau, String niveauOrganisation, java.time.LocalDate dateDebut,
                        java.time.LocalDate dateFin, java.util.List<Long> typesEtablissementIds,
                        java.util.List<String> typesEtablissement, String statutResultats,
                        java.time.LocalDate dateDeliberation, java.time.LocalDate dateProclamation) {

    public static ExamenDto de(ExamenNational e) {
        return new ExamenDto(e.getId(), e.getLibelle(), e.getAnneeScolaire().getId(),
                e.getCycle() == null ? null : e.getCycle().getId(), e.getCycle() == null ? null : e.getCycle().getLibelle(),
                e.getNiveau() == null ? null : e.getNiveau().getId(), e.getNiveau() == null ? null : e.getNiveau().getLibelle(),
                e.getNiveauOrganisation(), e.getDateDebut(), e.getDateFin(),
                e.getTypesEtablissement().stream().map(com.plateforme.scolaire.domain.TypeEtablissement::getId).toList(),
                e.getTypesEtablissement().stream().map(com.plateforme.scolaire.domain.TypeEtablissement::getLibelle).toList(),
                e.getStatutResultats(), e.getDateDeliberation(), e.getDateProclamation());
    }
}
