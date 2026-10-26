package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "note")
@Getter
@Setter
@NoArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id")
    private Inscription inscription;

    /** Null si l élève était absent. Règle à vérifier dans le service : valeur <= bareme. */
    @Column(precision = 5, scale = 2)
    private BigDecimal valeur;

    @Column(nullable = false)
    private boolean absent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifie_par")
    private Utilisateur modifiePar;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
