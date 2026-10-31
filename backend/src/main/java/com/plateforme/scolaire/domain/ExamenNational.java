package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un examen national d'une année scolaire, éventuellement propre à un cycle (primaire, collège, lycée). */
@Entity
@Table(name = "examen_national")
@Getter
@Setter
@NoArgsConstructor
public class ExamenNational {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private CycleScolaire cycle;

    /** Classe d'examen (6e année, 10e année, terminale…) ; facultatif. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niveau_id")
    private Niveau niveau;

    /** NATIONAL, ou un type de zone du référentiel (COMMUNE, PREFECTURE, REGION…) : qui organise l'examen. */
    @Column(name = "niveau_organisation", nullable = false, length = 50)
    private String niveauOrganisation = "NATIONAL";

    /** Période officielle des épreuves ; null tant qu'elle n'est pas publiée. */
    @Column(name = "date_debut")
    private java.time.LocalDate dateDebut;

    @Column(name = "date_fin")
    private java.time.LocalDate dateFin;

    /** EN_COURS, DELIBERE ou PROCLAME : avancement de la délibération et de la proclamation des résultats. */
    @Column(name = "statut_resultats", nullable = false, length = 10)
    private String statutResultats = "EN_COURS";

    @Column(name = "date_deliberation")
    private java.time.LocalDate dateDeliberation;

    @Column(name = "date_proclamation")
    private java.time.LocalDate dateProclamation;

    /** Types d'établissement qui participent ; vide : tous les types. */
    @ManyToMany
    @JoinTable(name = "examen_type_etablissement", joinColumns = @JoinColumn(name = "examen_id"),
            inverseJoinColumns = @JoinColumn(name = "type_etablissement_id"))
    private java.util.Set<TypeEtablissement> typesEtablissement = new java.util.LinkedHashSet<>();

    /** Un établissement sans type n'est pas concerné dès que l'examen restreint les types. */
    public boolean accepteType(TypeEtablissement type) {
        return typesEtablissement.isEmpty()
                || (type != null && typesEtablissement.stream().anyMatch(t -> t.getId().equals(type.getId())));
    }
}
