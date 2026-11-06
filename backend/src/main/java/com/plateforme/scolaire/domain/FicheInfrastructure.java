package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Infrastructures et équipements d'un établissement pour une année scolaire. */
@Entity
@Table(name = "fiche_infrastructure")
@Getter
@Setter
@NoArgsConstructor
public class FicheInfrastructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etablissement_id", nullable = false)
    private Long etablissementId;

    @Column(name = "annee_scolaire_id", nullable = false)
    private Long anneeScolaireId;

    @Column(name = "salles_dur", nullable = false)
    private int sallesDur;

    @Column(name = "salles_provisoires", nullable = false)
    private int sallesProvisoires;

    @Column(name = "salles_a_rehabiliter", nullable = false)
    private int sallesARehabiliter;

    @Column(name = "places_assises", nullable = false)
    private int placesAssises;

    @Column(name = "latrines_filles", nullable = false)
    private int latrinesFilles;

    @Column(name = "latrines_garcons", nullable = false)
    private int latrinesGarcons;

    @Column(name = "latrines_personnel", nullable = false)
    private int latrinesPersonnel;

    /** AUCUNE, PUITS, FORAGE ou RESEAU. */
    @Column(nullable = false, length = 10)
    private String eau = "AUCUNE";

    /** AUCUNE, RESEAU, SOLAIRE ou GROUPE. */
    @Column(nullable = false, length = 10)
    private String electricite = "AUCUNE";

    @Column(nullable = false)
    private boolean cloture;

    @Column(name = "lavage_mains", nullable = false)
    private boolean lavageMains;

    @Column(nullable = false)
    private boolean bibliotheque;

    @Column(nullable = false)
    private boolean laboratoire;

    @Column(name = "salle_informatique", nullable = false)
    private boolean salleInformatique;

    @Column(nullable = false)
    private boolean cantine;

    @Column(name = "terrain_sport", nullable = false)
    private boolean terrainSport;

    @Column(length = 500)
    private String observation;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    @Column(name = "mis_a_jour_par", length = 150)
    private String misAJourPar;
}
