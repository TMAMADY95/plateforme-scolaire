package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lieu où les candidats d'un examen composent ; rattaché à la zone qui organise l'examen. */
@Entity
@Table(name = "centre_examen")
@Getter
@Setter
@NoArgsConstructor
public class CentreExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneGeographique zone;

    /** Établissement scolaire désigné comme centre ; null pour un centre saisi librement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String adresse;

    /** Premier jour des épreuves dans ce centre ; facultatif. */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
