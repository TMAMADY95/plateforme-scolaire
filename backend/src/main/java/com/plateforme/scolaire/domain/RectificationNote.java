package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Trace d'une note rectifiée après proclamation : anciennes et nouvelles valeurs de la note, de la moyenne et de la décision. */
@Entity
@Table(name = "rectification_note")
@Getter
@Setter
@NoArgsConstructor
public class RectificationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reclamation_id")
    private ReclamationExamen reclamation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id")
    private CandidatExamen candidat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id")
    private EpreuveExamen epreuve;

    @Column(name = "ancienne_note", precision = 4, scale = 2)
    private BigDecimal ancienneNote;

    @Column(name = "nouvelle_note", nullable = false, precision = 4, scale = 2)
    private BigDecimal nouvelleNote;

    @Column(name = "ancienne_moyenne", precision = 5, scale = 2)
    private BigDecimal ancienneMoyenne;

    @Column(name = "nouvelle_moyenne", precision = 5, scale = 2)
    private BigDecimal nouvelleMoyenne;

    @Column(name = "ancienne_decision", length = 10)
    private String ancienneDecision;

    @Column(name = "nouvelle_decision", length = 10)
    private String nouvelleDecision;

    @Column(name = "rectifiee_le", nullable = false)
    private Instant rectifieeLe;

    @Column(name = "rectifiee_par", length = 150)
    private String rectifieePar;

    @Column(name = "annulee_le")
    private Instant annuleeLe;

    @Column(name = "annulee_par", length = 150)
    private String annuleePar;

    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;
}
