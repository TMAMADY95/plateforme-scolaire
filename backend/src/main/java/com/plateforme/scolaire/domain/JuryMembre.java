package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un membre du jury de délibération d'un examen, avec sa fonction (président, rapporteur…) et sa présence à la séance. */
@Entity
@Table(name = "jury_membre")
@Getter
@Setter
@NoArgsConstructor
public class JuryMembre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    private int ordre;

    @Column(nullable = false, length = 150)
    private String fonction;

    @Column(length = 150)
    private String nom;

    private boolean present;
}
