package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.RendezVous;
import java.time.LocalDateTime;

public record RendezVousDto(Long id, Long eleveId, String eleve, Long enseignantId, String enseignant,
                            LocalDateTime dateHeure, String motif, String statut, String reponse) {

    public static RendezVousDto de(RendezVous r) {
        var u = r.getEnseignant().getUtilisateur();
        return new RendezVousDto(r.getId(), r.getEleve().getId(), (r.getEleve().getPrenom() + " " + r.getEleve().getNom()).trim(),
                r.getEnseignant().getId(), ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim(),
                r.getDateHeure(), r.getMotif(), r.getStatut(), r.getReponse());
    }
}
