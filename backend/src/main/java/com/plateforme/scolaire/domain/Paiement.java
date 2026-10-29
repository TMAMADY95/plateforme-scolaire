package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un paiement sur une facture. Aucune donnée bancaire n'est conservée :
 * seulement notre référence et, le cas échéant, celle du prestataire.
 */
@Entity
@Table(name = "paiement")
@Getter
@Setter
@NoArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 3)
    private String devise;

    /** ESPECES, VIREMENT, CHEQUE ou MOBILE_MONEY */
    @Column(nullable = false, length = 20)
    private String mode;

    /** EN_ATTENTE, VALIDE, ECHEC ou ANNULE */
    @Column(nullable = false, length = 20)
    private String statut;

    @Column(name = "reference_interne", nullable = false, unique = true, length = 40)
    private String referenceInterne;

    @Column(name = "reference_externe", length = 100)
    private String referenceExterne;

    /** Adresse où le payeur termine son paiement en ligne ; permet de le reprendre tant qu'il est ouvert. */
    @Column(name = "url_paiement", length = 500)
    private String urlPaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enregistre_par")
    private Utilisateur enregistrePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "valide_le")
    private Instant valideLe;

    @Column(name = "annule_le")
    private Instant annuleLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par")
    private Utilisateur annulePar;

    @Column(name = "motif_annulation", length = 255)
    private String motifAnnulation;
}
