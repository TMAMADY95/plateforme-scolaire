package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Exemplaires d'un manuel reçus, distribués aux élèves et perdus ou hors d'usage dans un établissement pour une année. */
@Entity
@Table(name = "dotation_manuel")
@Getter
@Setter
@NoArgsConstructor
public class DotationManuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @Column(name = "annee_scolaire_id", nullable = false)
    private Long anneeScolaireId;

    @Column(name = "manuel_id", nullable = false)
    private Long manuelId;

    @Column(nullable = false)
    private int recus;

    @Column(nullable = false)
    private int distribues;

    @Column(nullable = false)
    private int perdus;

    @Column(length = 500)
    private String observation;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    @Column(name = "mis_a_jour_par", length = 150)
    private String misAJourPar;
}
