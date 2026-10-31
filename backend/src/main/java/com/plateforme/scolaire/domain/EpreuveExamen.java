package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une épreuve de l'emploi du temps officiel d'un examen : matière, date et créneau horaire. */
@Entity
@Table(name = "epreuve_examen")
@Getter
@Setter
@NoArgsConstructor
public class EpreuveExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    /** Matière du référentiel ; sinon [libelle] (épreuve qui n'a pas de matière propre). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @Column(length = 150)
    private String libelle;

    @Column(name = "date_epreuve", nullable = false)
    private LocalDate dateEpreuve;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    /** Poids de l'épreuve dans la moyenne pondérée du candidat (1 par défaut). */
    @Column(nullable = false)
    private int coefficient = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String nom() {
        return matiere != null ? matiere.getLibelle() : libelle;
    }
}
