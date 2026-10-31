package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Président, secrétaire ou correcteur d'une commission de correction. */
@Entity
@Table(name = "membre_commission_correction")
@Getter
@Setter
@NoArgsConstructor
public class MembreCommissionCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commission_id")
    private CommissionCorrection commission;

    @Column(nullable = false, length = 12)
    private String role;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 40)
    private String telephone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
