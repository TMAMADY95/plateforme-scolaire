package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Devoir;
import java.time.Instant;
import java.time.LocalDate;

/**
 * [fait] n'est renseigné que dans la liste d'un élève ; [faits] et [effectif] (élèves de la classe ayant terminé /
 * élèves de la classe) que dans la liste destinée à l'équipe pédagogique. Ailleurs, ils valent null.
 */
public record DevoirDto(Long id, Long classeId, Long matiereId, String matiere, String enseignant, String titre,
                        String consigne, LocalDate dateLimite, Instant creeLe,
                        Boolean fait, Integer faits, Integer effectif) {

    public static DevoirDto de(Devoir d) {
        var u = d.getEnseignant().getUtilisateur();
        return new DevoirDto(d.getId(), d.getClasse().getId(), d.getMatiere().getId(), d.getMatiere().getLibelle(),
                (u.getPrenom() + " " + u.getNom()).trim(), d.getTitre(), d.getConsigne(), d.getDateLimite(),
                d.getCreatedAt(), null, null, null);
    }

    public DevoirDto avecFait(boolean valeur) {
        return new DevoirDto(id, classeId, matiereId, matiere, enseignant, titre, consigne, dateLimite, creeLe,
                valeur, null, null);
    }

    public DevoirDto avecProgres(int termines, int total) {
        return new DevoirDto(id, classeId, matiereId, matiere, enseignant, titre, consigne, dateLimite, creeLe,
                null, termines, total);
    }
}
