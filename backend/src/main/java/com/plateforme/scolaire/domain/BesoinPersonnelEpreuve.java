package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nombre de personnes requises pour une fonction lors d'une épreuve donnée. */
@Entity
@Table(name = "besoin_personnel_epreuve")
@Getter
@Setter
@NoArgsConstructor
public class BesoinPersonnelEpreuve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epreuve_id", nullable = false)
    private Long epreuveId;

    @Column(nullable = false, length = 15)
    private String fonction;

    @Column(nullable = false)
    private int requis;
}
