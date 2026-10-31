package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "periode")
@Getter
@Setter
@NoArgsConstructor
public class Periode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    /** Null : période nationale. Sinon, période propre à cet établissement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** TRIMESTRE, SEMESTRE ou AUTRE */
    @Column(name = "type_periode", nullable = false, length = 10)
    private String typePeriode = "AUTRE";

    @Column(nullable = false, length = 50)
    private String libelle;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
}
