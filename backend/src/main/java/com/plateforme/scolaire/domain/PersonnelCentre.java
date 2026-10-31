package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une personne chargée d'une fonction dans un centre d'examen (éventuellement dans une salle). */
@Entity
@Table(name = "personnel_centre")
@Getter
@Setter
@NoArgsConstructor
public class PersonnelCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 40)
    private String telephone;

    /** Numéro de salle, pour les surveillants et les chefs de salle. */
    private Integer salle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
