package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** L'épreuve d'un examen telle qu'elle se déroule dans un centre : circuit du pli, ouverture et rapport de surveillance. */
@Entity
@Table(name = "epreuve_centre")
@Getter
@Setter
@NoArgsConstructor
public class EpreuveCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id")
    private EpreuveExamen epreuve;

    /** Nombre de sujets imprimés pour le centre : au moins un par candidat inscrit. */
    @Column(name = "nb_exemplaires", nullable = false)
    private int nbExemplaires;

    /** A_CONDITIONNER, CONDITIONNE, EXPEDIE_IRE, RECU_DPE, RECU_PRESIDENT puis OUVERT. */
    @Column(name = "statut_pli", nullable = false, length = 15)
    private String statutPli = "A_CONDITIONNER";

    @Column(name = "conditionne_le")
    private Instant conditionneLe;

    @Column(name = "expedie_le")
    private Instant expedieLe;

    @Column(name = "recu_dpe_le")
    private Instant recuDpeLe;

    @Column(name = "recu_president_le")
    private Instant recuPresidentLe;

    @Column(name = "ouvert_le")
    private Instant ouvertLe;

    @Column(name = "ouvert_par", length = 150)
    private String ouvertPar;

    @Column(length = 150)
    private String temoin1;

    @Column(length = 150)
    private String temoin2;

    @Column(name = "composition_debut")
    private Instant compositionDebut;

    @Column(name = "composition_fin")
    private Instant compositionFin;

    private Integer presents;

    private Integer absents;

    private Integer elimines;

    @Column(name = "copies_ramassees")
    private Integer copiesRamassees;

    @Column(length = 2000)
    private String observations;

    @Column(name = "rapport_clos", nullable = false)
    private boolean rapportClos;
}
