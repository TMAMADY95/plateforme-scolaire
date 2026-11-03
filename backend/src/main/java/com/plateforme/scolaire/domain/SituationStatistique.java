package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Photographie datée des statistiques des examens d'une année : provisoire, ou définitive (une seule par année). */
@Entity
@Table(name = "situation_statistique")
@Getter
@Setter
@NoArgsConstructor
public class SituationStatistique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    /** PROVISOIRE ou DEFINITIVE. */
    @Column(nullable = false, length = 10)
    private String statut;

    @Column(name = "etablie_le", nullable = false)
    private Instant etablieLe;

    @Column(name = "etablie_par", length = 150)
    private String etabliePar;

    @Column(length = 500)
    private String commentaire;

    @Column(nullable = false)
    private int inscrits;

    @Column(nullable = false)
    private int filles;

    @Column(nullable = false)
    private int libres;

    @Column(nullable = false)
    private int presents;

    @Column(nullable = false)
    private int absents;

    @Column(nullable = false)
    private int elimines;

    @Column(nullable = false)
    private int admis;

    @Column(nullable = false)
    private int centres;

    @Column(nullable = false)
    private int salles;

    @OneToMany(mappedBy = "situation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("niveau")
    private List<SituationStatistiqueNiveau> niveaux = new ArrayList<>();
}
