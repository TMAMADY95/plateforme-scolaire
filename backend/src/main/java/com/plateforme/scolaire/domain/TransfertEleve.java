package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Transfert d'un élève en cours d'année : DEMANDE, puis APPROUVE ou REFUSE par l'école d'origine, ou ANNULE par l'école d'accueil. */
@Entity
@Table(name = "transfert_eleve")
@Getter
@Setter
@NoArgsConstructor
public class TransfertEleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eleve_id", nullable = false)
    private Long eleveId;

    @Column(name = "annee_scolaire_id", nullable = false)
    private Long anneeScolaireId;

    @Column(name = "etablissement_origine_id", nullable = false)
    private Long etablissementOrigineId;

    @Column(name = "etablissement_dest_id", nullable = false)
    private Long etablissementDestId;

    /** DEMENAGEMENT, RAPPROCHEMENT_FAMILIAL, ORIENTATION, DISCIPLINE ou AUTRE. */
    @Column(nullable = false, length = 25)
    private String motif;

    @Column(length = 500)
    private String commentaire;

    @Column(nullable = false, length = 10)
    private String statut = "DEMANDE";

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @Column(name = "demande_le", nullable = false)
    private LocalDate demandeLe;

    @Column(name = "demande_par", length = 150)
    private String demandePar;

    @Column(name = "traite_le")
    private LocalDate traiteLe;

    @Column(name = "traite_par", length = 150)
    private String traitePar;

    @Column(name = "nouvelle_inscription_id")
    private Long nouvelleInscriptionId;
}
