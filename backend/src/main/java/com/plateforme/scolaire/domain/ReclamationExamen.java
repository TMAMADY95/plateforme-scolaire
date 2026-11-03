package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réclamation d'un candidat sur ses résultats : déposée puis acceptée ou rejetée par la DGECS dans un délai. */
@Entity
@Table(name = "reclamation_examen")
@Getter
@Setter
@NoArgsConstructor
public class ReclamationExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id")
    private CandidatExamen candidat;

    /** NOTE, IDENTITE, OMISSION ou AUTRE. */
    @Column(nullable = false, length = 20)
    private String objet;

    @Column(nullable = false, length = 2000)
    private String description;

    /** OUVERTE, puis ACCEPTEE ou REJETEE. */
    @Column(nullable = false, length = 10)
    private String statut = "OUVERTE";

    @Column(name = "deposee_le", nullable = false)
    private Instant deposeeLe;

    @Column(name = "echeance_le", nullable = false)
    private Instant echeanceLe;

    @Column(name = "traitee_le")
    private Instant traiteeLe;

    @Column(length = 1000)
    private String decision;
}
