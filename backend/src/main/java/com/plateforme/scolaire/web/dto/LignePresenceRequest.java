package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** motif et justifie ne s'appliquent qu'aux absences et aux retards. */
public record LignePresenceRequest(@NotNull Long inscriptionId,
                                   @NotNull StatutPresence statut,
                                   @Size(max = 255) String motif,
                                   Boolean justifie) {
}
