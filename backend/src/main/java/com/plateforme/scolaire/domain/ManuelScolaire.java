package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Manuel du programme officiel : un titre pour une matière et un niveau. */
@Entity
@Table(name = "manuel_scolaire")
@Getter
@Setter
@NoArgsConstructor
public class ManuelScolaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niveau_id")
    private Niveau niveau;

    /** Série du lycée concernée ; null = toutes les séries du niveau. */
    @Column(length = 30)
    private String serie;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 150)
    private String editeur;

    @Column(name = "annee_edition")
    private Integer anneeEdition;
}
