package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Élève qui a quitté l'école en cours d'année, avec le motif déclaré par la direction. */
@Entity
@Table(name = "abandon_scolaire")
@Getter
@Setter
@NoArgsConstructor
public class AbandonScolaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscription_id", nullable = false, unique = true)
    private Long inscriptionId;

    @Column(name = "eleve_id", nullable = false)
    private Long eleveId;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @Column(name = "annee_scolaire_id", nullable = false)
    private Long anneeScolaireId;

    @Column(name = "date_abandon", nullable = false)
    private LocalDate dateAbandon;

    @Column(nullable = false, length = 30)
    private String motif;

    @Column(length = 500)
    private String observation;

    @Column(name = "signale_par", length = 255)
    private String signalePar;

    @Column(name = "signale_le", nullable = false)
    private Instant signaleLe = Instant.now();
}
