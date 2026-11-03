package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Acheminement des sujets et du matériel vers un centre d'examen : véhicule, itinéraire, horaires prévus et réels, carburant, incident éventuel. */
@Entity
@Table(name = "convoi_examen")
@Getter
@Setter
@NoArgsConstructor
public class ConvoiExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @Column(nullable = false, length = 60)
    private String vehicule;

    @Column(length = 100)
    private String chauffeur;

    @Column(length = 300)
    private String itineraire;

    @Column(nullable = false)
    private boolean escorte;

    @Column(name = "depart_prevu")
    private Instant departPrevu;

    @Column(name = "arrivee_prevue")
    private Instant arriveePrevue;

    @Column(name = "depart_reel")
    private Instant departReel;

    @Column(name = "arrivee_reelle")
    private Instant arriveeReelle;

    @Column(name = "carburant_litres")
    private Integer carburantLitres;

    /** PREVU, EN_ROUTE ou LIVRE. */
    @Column(nullable = false, length = 10)
    private String statut = "PREVU";

    /** PERTE, RETARD, ERREUR_DISTRIBUTION, RUPTURE_MATERIEL, ACCIDENT ou AUTRE ; null sans incident. */
    @Column(name = "incident_type", length = 25)
    private String incidentType;

    @Column(name = "incident_description", length = 500)
    private String incidentDescription;

    @Column(length = 500)
    private String observation;
}
