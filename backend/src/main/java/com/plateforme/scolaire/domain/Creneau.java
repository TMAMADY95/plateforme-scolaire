package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

/**
 * Un créneau de l'emploi du temps d'une classe : une matière, un enseignant et une salle
 * sur un jour de la semaine et un intervalle horaire.
 */
@Entity
@Table(name = "creneau")
@Getter
@Setter
@NoArgsConstructor
public class Creneau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    /** Peut rester vide tant que l'enseignant n'est pas affecté. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id")
    private Salle salle;

    /** 1 = lundi ... 7 = dimanche. */
    @Column(name = "jour_semaine", nullable = false)
    private Short jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;
}
