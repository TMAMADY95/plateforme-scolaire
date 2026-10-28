package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Paiement;
import java.math.BigDecimal;
import java.time.Instant;

public record PaiementDto(Long id, Long factureId, BigDecimal montant, String devise, String mode, String statut,
                          String referenceInterne, String referenceExterne, Instant createdAt, Instant valideLe) {

    public static PaiementDto de(Paiement p) {
        return new PaiementDto(p.getId(), p.getFacture().getId(), p.getMontant(), p.getDevise(), p.getMode(),
                p.getStatut(), p.getReferenceInterne(), p.getReferenceExterne(), p.getCreatedAt(), p.getValideLe());
    }
}
