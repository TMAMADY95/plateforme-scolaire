package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un incident signalé dans un centre d'examen (fraude, retard, malaise, perturbation, fuite de sujet…). */
@Entity
@Table(name = "incident_examen")
@Getter
@Setter
@NoArgsConstructor
public class IncidentExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidat_id")
    private CandidatExamen candidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epreuve_id")
    private EpreuveExamen epreuve;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "survenu_le", nullable = false)
    private Instant survenuLe;

    @Column(name = "signale_par", length = 150)
    private String signalePar;

    /** OUVERT puis TRAITE quand une décision est consignée. */
    @Column(nullable = false, length = 10)
    private String statut = "OUVERT";

    @Column(length = 1000)
    private String decision;

    /** Vrai quand l'incident a entraîné l'élimination du candidat (fraude). */
    @Column(nullable = false)
    private boolean elimine;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
