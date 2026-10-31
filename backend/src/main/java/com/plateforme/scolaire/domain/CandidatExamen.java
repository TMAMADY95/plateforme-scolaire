package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un élève inscrit à un examen par son établissement. */
@Entity
@Table(name = "candidat_examen")
@Getter
@Setter
@NoArgsConstructor
public class CandidatExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    /** Candidat libre : non scolarisé dans une classe de l'année, candidature déposée par un établissement (BEPC et BAC). */
    @Column(nullable = false)
    private boolean libre;

    /** Numéro de candidat, attribué à l'inscription. */
    @Column(length = 30)
    private String numero;

    /** Numéro de procès-verbal : identifie le candidat pendant la surveillance, la correction et la délibération. */
    @Column(name = "numero_pv", length = 40)
    private String numeroPv;

    /** Salle (1, 2, 3…) et table (1 à 30) dans le centre. */
    private Integer salle;

    @Column(name = "table_numero")
    private Integer tableNumero;

    /** INSCRIT (pas encore composé), PRESENT, ABSENT ou ELIMINE (fraude). */
    @Column(name = "statut_epreuves", nullable = false, length = 10)
    private String statutEpreuves = "INSCRIT";

    /** Moyenne pondérée sur 20, calculée quand toutes les épreuves sont notées. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal moyenne;

    /** ADMIS ou NON_ADMIS ; null tant que le résultat n'est pas établi. */
    @Column(length = 10)
    private String decision;

    /** Notes sur 20 par épreuve (identifiant de l'épreuve -> note). */
    @ElementCollection
    @CollectionTable(name = "note_examen", joinColumns = @JoinColumn(name = "candidat_id"))
    @MapKeyColumn(name = "epreuve_id")
    @Column(name = "note", precision = 4, scale = 2)
    @org.hibernate.annotations.BatchSize(size = 100)
    private java.util.Map<Long, java.math.BigDecimal> notes = new java.util.HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
