package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Diplôme délivré à un candidat admis, avec son numéro et son code de vérification publique. */
@Entity
@Table(name = "diplome_examen")
@Getter
@Setter
@NoArgsConstructor
public class DiplomeExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id")
    private CandidatExamen candidat;

    @Column(nullable = false, length = 40)
    private String numero;

    @Column(nullable = false, length = 12)
    private String code;

    /** PASSABLE, ASSEZ_BIEN, BIEN ou TRES_BIEN. */
    @Column(nullable = false, length = 12)
    private String mention;

    @Column(name = "delivre_le", nullable = false)
    private LocalDate delivreLe;

    @Column(nullable = false)
    private boolean revoque;

    @Column(name = "motif_revocation", length = 500)
    private String motifRevocation;

    @Column(name = "revoque_le")
    private Instant revoqueLe;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
