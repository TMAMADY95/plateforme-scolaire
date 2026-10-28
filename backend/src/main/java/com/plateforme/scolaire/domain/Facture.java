package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Une facture émise pour un élève, à partir d'un frais.
 * Le libellé et le montant dû sont recopiés du frais à l'émission : modifier
 * la grille tarifaire plus tard ne change pas les factures déjà émises.
 */
@Entity
@Table(name = "facture")
@Getter
@Setter
@NoArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "frais_id")
    private Frais frais;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "montant_du", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantDu = BigDecimal.ZERO;

    @Column(name = "montant_exonere", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantExonere = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String devise;

    private LocalDate echeance;

    /** IMPAYEE, PARTIELLE, PAYEE, EXONEREE ou ANNULEE */
    @Column(nullable = false, length = 20)
    private String statut = "IMPAYEE";

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Montant restant dû, exonération déduite. */
    public BigDecimal resteAPayer() {
        return montantDu.subtract(montantExonere).subtract(montantPaye);
    }

    /** Somme des paiements encaissés (Étape 3b). */
    @Transient
    private BigDecimal montantPaye = BigDecimal.ZERO;
}
