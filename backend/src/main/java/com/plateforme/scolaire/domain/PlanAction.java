package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mesure à prendre par un établissement après une inspection, avec une échéance. */
@Entity
@Table(name = "plan_action")
@Getter
@Setter
@NoArgsConstructor
public class PlanAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_id")
    private Utilisateur creePar;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate echeance;

    /** A_FAIRE, EN_COURS, TERMINE ou ANNULE */
    @Column(nullable = false, length = 10)
    private String statut = "A_FAIRE";

    @Column(length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "termine_le")
    private Instant termineLe;

    @Column(name = "dernier_rappel")
    private Instant dernierRappel;
}
