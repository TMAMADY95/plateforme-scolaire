package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une variante (A, B ou C) du sujet d'une épreuve : on suit son état, jamais son contenu. */
@Entity
@Table(name = "sujet_examen")
@Getter
@Setter
@NoArgsConstructor
public class SujetExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id")
    private EpreuveExamen epreuve;

    @Column(nullable = false, length = 1)
    private String variante;

    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(length = 300)
    private String auteurs;

    /** ELABORATION, VALIDE (en réserve), TIRE (utilisé) ou COMPROMIS (fuite). */
    @Column(nullable = false, length = 12)
    private String statut = "ELABORATION";

    @Column(name = "valide_le")
    private Instant valideLe;

    @Column(name = "tire_le")
    private Instant tireLe;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
