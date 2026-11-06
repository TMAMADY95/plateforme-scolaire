package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Décision du conseil de classe pour un élève, avec la moyenne annuelle et la suggestion telles qu'elles étaient au moment de la décision. */
@Entity
@Table(name = "decision_fin_annee")
@Getter
@Setter
@NoArgsConstructor
public class DecisionFinAnnee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscription_id", nullable = false, unique = true)
    private Long inscriptionId;

    @Column(name = "classe_id", nullable = false)
    private Long classeId;

    @Column(name = "moyenne_annuelle", precision = 5, scale = 2)
    private BigDecimal moyenneAnnuelle;

    /** ADMIS, A_DELIBERER ou REDOUBLE ; null sans moyenne. */
    @Column(length = 12)
    private String suggestion;

    /** PASSAGE, REDOUBLEMENT ou EXCLUSION. */
    @Column(nullable = false, length = 12)
    private String decision;

    @Column(length = 500)
    private String observation;

    @Column(name = "decide_le", nullable = false)
    private LocalDate decideLe;

    @Column(name = "decide_par", length = 150)
    private String decidePar;
}
