package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Situation de l'élève suivie pour l'annuaire statistique : orphelin (père, mère, double) et type de handicap. */
@Entity
@Table(name = "profil_vulnerabilite_eleve")
@Getter
@Setter
@NoArgsConstructor
public class ProfilVulnerabiliteEleve {

    @Id
    @Column(name = "eleve_id")
    private Long eleveId;

    /** AUCUN, PERE, MERE ou DEUX (orphelin de père, de mère ou des deux). */
    @Column(nullable = false, length = 10)
    private String orphelin = "AUCUN";

    @Column(length = 15)
    private String handicap;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe = Instant.now();

    @Column(name = "mis_a_jour_par", length = 255)
    private String misAJourPar;
}
