package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "etablissement")
@Getter
@Setter
@NoArgsConstructor
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_officiel", nullable = false, unique = true, length = 50)
    private String codeOfficiel;

    @Column(nullable = false, length = 200)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneGeographique zone;

    /** PUBLIC ou PRIVE */
    @Column(nullable = false, length = 10)
    private String secteur = "PUBLIC";

    /** Type du référentiel (communautaire, franco-arabe…) ; facultatif. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_etablissement_id")
    private TypeEtablissement typeEtablissement;

    /** Cycles proposés : un seul ou plusieurs (primaire, collège, lycée…). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "etablissement_cycle",
            joinColumns = @JoinColumn(name = "etablissement_id"),
            inverseJoinColumns = @JoinColumn(name = "cycle_id"))
    private Set<CycleScolaire> cycles = new LinkedHashSet<>();

    @Column(length = 300)
    private String adresse;

    private Integer capacite;

    /** ACTIF, SUSPENDU ou FERME */
    @Column(nullable = false, length = 20)
    private String statut = "ACTIF";

    /** Postes d'enseignants prévus ; null tant que non renseigné. */
    @Column(name = "postes_enseignants")
    private Integer postesEnseignants;

    /** EN_ATTENTE, AUTORISE, NON_AUTORISE ou RETIRE */
    @Column(name = "autorisation_statut", nullable = false, length = 20)
    private String autorisationStatut = "EN_ATTENTE";

    @Column(name = "autorisation_reference", length = 100)
    private String autorisationReference;

    @Column(name = "autorisation_date")
    private java.time.LocalDate autorisationDate;

    /** Raison d'une suspension, d'une fermeture ou d'un retrait d'autorisation. */
    @Column(name = "motif_statut", length = 500)
    private String motifStatut;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
