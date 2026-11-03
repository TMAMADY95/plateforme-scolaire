package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Caractéristiques d'un centre d'examen : salles de réserve, état des installations, accessibilité, distances et risques. */
@Entity
@Table(name = "caracteristiques_centre")
@Getter
@Setter
@NoArgsConstructor
public class CaracteristiquesCentre {

    @Id
    @Column(name = "centre_id")
    private Long centreId;

    @Column(name = "salles_reserve", nullable = false)
    private int sallesReserve;

    /** BON, MOYEN ou MAUVAIS ; null si non évalué. */
    @Column(name = "etat_tables_bancs", length = 10)
    private String etatTablesBancs;

    @Column(name = "etat_toilettes", length = 10)
    private String etatToilettes;

    @Column(name = "etat_eclairage", length = 10)
    private String etatEclairage;

    @Column(name = "etat_ventilation", length = 10)
    private String etatVentilation;

    private Boolean accessible;

    @Column(name = "distance_max_km")
    private Integer distanceMaxKm;

    @Column(name = "temps_max_min")
    private Integer tempsMaxMin;

    @Column(name = "risque_inondation", nullable = false)
    private boolean risqueInondation;

    @Column(name = "risque_enclavement", nullable = false)
    private boolean risqueEnclavement;

    @Column(name = "risque_insecurite", nullable = false)
    private boolean risqueInsecurite;

    @Column(length = 500)
    private String observation;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;
}
