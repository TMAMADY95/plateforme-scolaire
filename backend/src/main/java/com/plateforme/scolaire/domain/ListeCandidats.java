package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Liste des candidats d'un établissement pour un examen, et son état dans le circuit de validation. */
@Entity
@Table(name = "liste_candidats")
@Getter
@Setter
@NoArgsConstructor
public class ListeCandidats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** BROUILLON, TRANSMISE, VALIDEE_REGION ou VALIDEE. */
    @Column(nullable = false, length = 15)
    private String statut = "BROUILLON";

    @Column(name = "transmise_le")
    private Instant transmiseLe;

    @Column(name = "validee_region_le")
    private Instant valideeRegionLe;

    @Column(name = "validee_le")
    private Instant valideeLe;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;
}
