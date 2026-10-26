package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Presence;
import java.time.LocalDate;

public record PresenceDto(Long id, Long inscriptionId, Long classeId, LocalDate date, String statut,
                          String motif, boolean justifie) {

    public static PresenceDto de(Presence p) {
        return new PresenceDto(
                p.getId(),
                p.getInscription().getId(),
                p.getInscription().getClasse() != null ? p.getInscription().getClasse().getId() : null,
                p.getDateSeance(),
                p.getStatut(),
                p.getMotif(),
                p.isJustifie());
    }
}
