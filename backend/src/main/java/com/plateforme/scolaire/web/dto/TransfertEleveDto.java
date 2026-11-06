package com.plateforme.scolaire.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Transferts d'élèves d'un établissement : demandes reçues (à décider) et demandes émises (suivies). */
public record TransfertEleveDto(Long etablissementId, Long anneeId, List<Transfert> sortants, List<Transfert> entrants) {

    public record Transfert(Long id, Long eleveId, String matricule, String eleve, Long origineId, String origine, Long destinationId, String destination,
            String motif, String commentaire, String statut, String motifRefus, LocalDate demandeLe, String demandePar, LocalDate traiteLe,
            String traitePar, Long nouvelleInscriptionId) {
    }

    public record Demande(@NotBlank @Size(max = 50) String matriculeEleve, @NotNull Long anneeId, @NotBlank @Size(max = 25) String motif,
            @Size(max = 500) String commentaire) {
    }

    /** decision : APPROUVE ou REFUSE ; le motif du refus est obligatoire dans ce cas. */
    public record Decision(@NotBlank @Size(max = 10) String decision, @Size(max = 500) String motifRefus) {
    }
}
