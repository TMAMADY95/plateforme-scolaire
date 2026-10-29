package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Poids d'une matière dans la moyenne générale d'une classe. */
@Entity
@Table(name = "coefficient_matiere")
@Getter
@Setter
@NoArgsConstructor
public class CoefficientMatiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal coefficient;
}
