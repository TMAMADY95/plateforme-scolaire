package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Demande d'un parent pour qu'un élève quitte l'école avant la fin des cours ; la direction l'accepte ou la refuse. */
@Entity
@Table(name = "autorisation_sortie")
@Getter
@Setter
@NoArgsConstructor
public class AutorisationSortie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandee_par_id")
    private Utilisateur demandeePar;

    @Column(name = "date_sortie", nullable = false)
    private LocalDate dateSortie;

    @Column(name = "heure_sortie", nullable = false)
    private LocalTime heureSortie;

    @Column(nullable = false, length = 500)
    private String motif;

    /** Qui vient chercher l'élève. */
    @Column(length = 150)
    private String accompagnateur;

    /** DEMANDEE, ACCEPTEE ou REFUSEE */
    @Column(nullable = false, length = 10)
    private String statut = "DEMANDEE";

    @Column(length = 500)
    private String reponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traitee_par_id")
    private Utilisateur traiteePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "traitee_le")
    private Instant traiteeLe;
}
