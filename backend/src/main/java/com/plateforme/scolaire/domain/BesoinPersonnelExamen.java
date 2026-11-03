package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nombre de personnes requises pour une fonction donnée dans un examen. */
@Entity
@Table(name = "besoin_personnel_examen")
@Getter
@Setter
@NoArgsConstructor
public class BesoinPersonnelExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @Column(nullable = false, length = 15)
    private String fonction;

    @Column(nullable = false)
    private int requis;
}
