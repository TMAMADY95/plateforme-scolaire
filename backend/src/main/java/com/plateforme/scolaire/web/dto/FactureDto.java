package com.plateforme.scolaire.web.dto;

import com.plateforme.scolaire.domain.Facture;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Une facture d'élève, avec son solde. */
public record FactureDto(Long id, Long fraisId, String numero, String libelle,
                         Long eleveId, String eleveNom, String elevePrenom, String matricule,
                         Long etablissementId, Long anneeScolaireId,
                         BigDecimal montantDu, BigDecimal montantExonere, BigDecimal montantPaye,
                         BigDecimal resteAPayer, String devise, LocalDate echeance, String statut,
                         LocalDate dateEmission, boolean enRetard) {

    public static FactureDto de(Facture f) {
        BigDecimal paye = f.getMontantPaye() == null ? BigDecimal.ZERO : f.getMontantPaye();
        BigDecimal reste = f.getMontantDu().subtract(f.getMontantExonere()).subtract(paye);
        boolean enRetard = f.getEcheance() != null
                && f.getEcheance().isBefore(LocalDate.now())
                && ("IMPAYEE".equals(f.getStatut()) || "PARTIELLE".equals(f.getStatut()));
        return new FactureDto(
                f.getId(), f.getFrais().getId(), f.getNumero(), f.getLibelle(),
                f.getEleve().getId(), f.getEleve().getNom(), f.getEleve().getPrenom(), f.getEleve().getMatricule(),
                f.getEtablissement().getId(), f.getAnneeScolaire().getId(),
                f.getMontantDu(), f.getMontantExonere(), paye, reste, f.getDevise(),
                f.getEcheance(), f.getStatut(), f.getDateEmission(), enRetard);
    }
}
