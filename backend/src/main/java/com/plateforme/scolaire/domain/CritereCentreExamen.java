package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conditions pour qu'un établissement soit désigné centre d'examen (ligne unique, id = 1). */
@Entity
@Table(name = "critere_centre_examen")
@Getter
@Setter
@NoArgsConstructor
public class CritereCentreExamen {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    /** TOUS ou PUBLIC. */
    @Column(name = "secteur_admis", nullable = false, length = 10)
    private String secteurAdmis = "TOUS";

    @Column(name = "autorisation_requise", nullable = false)
    private boolean autorisationRequise;

    @Column(name = "salles_minimum", nullable = false)
    private int sallesMinimum;

    /** Somme des capacités des salles de l'établissement. */
    @Column(name = "capacite_minimum", nullable = false)
    private int capaciteMinimum;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
