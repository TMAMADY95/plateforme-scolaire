package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Incident disciplinaire d'un élève : OUVERT, puis SANCTIONNE par la direction ou CLASSE sans suite. */
@Entity
@Table(name = "incident_disciplinaire")
@Getter
@Setter
@NoArgsConstructor
public class IncidentDisciplinaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eleve_id", nullable = false)
    private Long eleveId;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @Column(name = "annee_scolaire_id", nullable = false)
    private Long anneeScolaireId;

    @Column(name = "date_faits", nullable = false)
    private LocalDate dateFaits;

    /** INSOLENCE, VIOLENCE, HARCELEMENT, FRAUDE, DEGRADATION, VOL, TELEPHONE, TENUE ou AUTRE. */
    @Column(nullable = false, length = 20)
    private String type;

    /** LEGERE, GRAVE ou TRES_GRAVE. */
    @Column(nullable = false, length = 12)
    private String gravite;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 10)
    private String statut = "OUVERT";

    /** AVERTISSEMENT, BLAME, RETENUE, EXCLUSION_TEMPORAIRE, CONSEIL_DISCIPLINE ou EXCLUSION_DEFINITIVE. */
    @Column(length = 25)
    private String sanction;

    @Column(name = "exclusion_jours")
    private Integer exclusionJours;

    @Column(name = "exclusion_debut")
    private LocalDate exclusionDebut;

    @Column(length = 500)
    private String observation;

    @Column(name = "parents_informes", nullable = false)
    private boolean parentsInformes;

    @Column(name = "signale_par", length = 150)
    private String signalePar;

    @Column(name = "signale_le", nullable = false)
    private LocalDate signaleLe;

    @Column(name = "decide_par", length = 150)
    private String decidePar;

    @Column(name = "decide_le")
    private LocalDate decideLe;
}
