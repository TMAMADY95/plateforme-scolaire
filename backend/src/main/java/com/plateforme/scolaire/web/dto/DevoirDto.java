package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Devoir;
import java.time.Instant;
import java.time.LocalDate;

public record DevoirDto(Long id, Long classeId, Long matiereId, String matiere, String enseignant, String titre,
                        String consigne, LocalDate dateLimite, Instant creeLe) {

    public static DevoirDto de(Devoir d) {
        var u = d.getEnseignant().getUtilisateur();
        return new DevoirDto(d.getId(), d.getClasse().getId(), d.getMatiere().getId(), d.getMatiere().getLibelle(),
                (u.getPrenom() + " " + u.getNom()).trim(), d.getTitre(), d.getConsigne(), d.getDateLimite(),
                d.getCreatedAt());
    }
}
