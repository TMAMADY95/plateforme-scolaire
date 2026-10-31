package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.CahierTexte;
import java.time.LocalDate;

public record CahierTexteDto(Long id, Long classeId, Long matiereId, String matiere, String enseignant,
                             LocalDate dateSeance, String contenu) {

    public static CahierTexteDto de(CahierTexte c) {
        var u = c.getEnseignant().getUtilisateur();
        return new CahierTexteDto(c.getId(), c.getClasse().getId(), c.getMatiere().getId(), c.getMatiere().getLibelle(),
                ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim(),
                c.getDateSeance(), c.getContenu());
    }
}
