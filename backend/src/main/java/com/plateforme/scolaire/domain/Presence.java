package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "presence")
@Getter
@Setter
@NoArgsConstructor
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id")
    private Inscription inscription;

    @Column(name = "date_seance", nullable = false)
    private LocalDate dateSeance;

    /** PRESENT, ABSENT ou RETARD */
    @Column(nullable = false, length = 10)
    private String statut;

    @Column(length = 255)
    private String motif;

    @Column(nullable = false)
    private boolean justifie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saisi_par")
    private Utilisateur saisiPar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
