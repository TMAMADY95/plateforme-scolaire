package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Effectifs d'un niveau d'examen figés dans une situation statistique. */
@Entity
@Table(name = "situation_statistique_niveau")
@IdClass(SituationStatistiqueNiveau.Cle.class)
@Getter
@Setter
@NoArgsConstructor
public class SituationStatistiqueNiveau {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "situation_id")
    private SituationStatistique situation;

    @Id
    @Column(nullable = false, length = 100)
    private String niveau;

    @Column(nullable = false)
    private int inscrits;

    @Column(nullable = false)
    private int filles;

    @Column(nullable = false)
    private int presents;

    @Column(nullable = false)
    private int admis;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Cle implements Serializable {
        private Long situation;
        private String niveau;
    }
}
