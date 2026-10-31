package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Commission de correction d'un examen : générale (sans épreuve) ou sous-commission d'une discipline. */
@Entity
@Table(name = "commission_correction")
@Getter
@Setter
@NoArgsConstructor
public class CommissionCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epreuve_id")
    private EpreuveExamen epreuve;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 150)
    private String lieu;

    @Column(name = "nb_copies", nullable = false)
    private int nbCopies;

    @Column(name = "nb_copies_corrigees", nullable = false)
    private int nbCopiesCorrigees;

    @Column(nullable = false)
    private boolean cloturee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
