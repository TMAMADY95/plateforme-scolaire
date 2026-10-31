package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CandidatExamen;

public record CandidatDto(Long id, Long eleveId, String nom, String matricule, String classe, String numero,
                          Long centreId, String centre, String numeroPv, Integer salle, Integer table, boolean libre) {

    public static CandidatDto de(CandidatExamen c) {
        var e = c.getEleve();
        return new CandidatDto(c.getId(), e.getId(), (e.getPrenom() + " " + e.getNom()).trim(), e.getMatricule(),
                c.getClasse() == null ? null : c.getClasse().getNom(), c.getNumero(),
                c.getCentre() == null ? null : c.getCentre().getId(), c.getCentre() == null ? null : c.getCentre().getNom(),
                c.getNumeroPv(), c.getSalle(), c.getTableNumero(), c.isLibre());
    }
}
