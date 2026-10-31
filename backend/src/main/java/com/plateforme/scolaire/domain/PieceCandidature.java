package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une pièce du dossier de candidature, reçue par l'établissement. */
@Entity
@Table(name = "piece_candidature")
@Getter
@Setter
@NoArgsConstructor
public class PieceCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id")
    private CandidatExamen candidat;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 150)
    private String reference;

    @Column(name = "fournie_le", nullable = false)
    private LocalDate fournieLe;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
