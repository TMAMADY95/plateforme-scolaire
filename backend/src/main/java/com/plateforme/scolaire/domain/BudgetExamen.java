package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ligne de budget d'un examen : montants prévu, engagé et payé (francs guinéens) pour une rubrique. */
@Entity
@Table(name = "budget_examen")
@Getter
@Setter
@NoArgsConstructor
public class BudgetExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @Column(nullable = false, length = 20)
    private String rubrique;

    @Column(name = "montant_prevu", nullable = false)
    private long montantPrevu;

    @Column(name = "montant_engage", nullable = false)
    private long montantEngage;

    @Column(name = "montant_paye", nullable = false)
    private long montantPaye;

    @Column(length = 500)
    private String observation;
}
