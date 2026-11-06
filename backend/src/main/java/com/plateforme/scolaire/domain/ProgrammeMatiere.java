package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ligne du programme : une matière d'un niveau (et d'une série) pour une année scolaire, avec son horaire et son coefficient. */
@Entity
@Table(name = "programme_matiere")
@Getter
@Setter
@NoArgsConstructor
public class ProgrammeMatiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niveau_id")
    private Niveau niveau;

    /** Série du lycée ; null = toutes les classes du niveau. */
    @Column(length = 30)
    private String serie;

    /** Type d'établissement concerné ; null = tous les types. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_etablissement_id")
    private TypeEtablissement typeEtablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @Column(name = "heures_hebdo", precision = 4, scale = 2)
    private BigDecimal heuresHebdo;

    @Column(precision = 4, scale = 2)
    private BigDecimal coefficient;

    @Column(length = 300)
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
