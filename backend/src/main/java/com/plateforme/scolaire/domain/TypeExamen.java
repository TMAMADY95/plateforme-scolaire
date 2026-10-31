package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Modèle d'examen (CEE, BEPC, baccalauréat…) : proposé à la création d'un examen, avec son niveau d'organisation. */
@Entity
@Table(name = "type_examen")
@Getter
@Setter
@NoArgsConstructor
public class TypeExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    /** NATIONAL ou un type de zone du référentiel. */
    @Column(name = "niveau_organisation", nullable = false, length = 50)
    private String niveauOrganisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niveau_id")
    private Niveau niveau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private CycleScolaire cycle;
}
