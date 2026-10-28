package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

/** Une remise ou une bourse accordée sur une facture. */
@Entity
@Table(name = "exoneration")
@Getter
@Setter
@NoArgsConstructor
public class Exoneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 255)
    private String motif;

    /** BOURSE, REMISE, AIDE_SOCIALE ou CORRECTION */
    @Column(name = "type_exoneration", nullable = false, length = 30)
    private String typeExoneration = "REMISE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decide_par")
    private Utilisateur decidePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
