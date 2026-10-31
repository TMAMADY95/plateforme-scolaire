package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rendez-vous demandé par un parent (ou l'élève) à un enseignant de la classe de l'élève. */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_par_id")
    private Utilisateur demandePar;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false, length = 500)
    private String motif;

    /** DEMANDE, CONFIRME, REFUSE ou ANNULE */
    @Column(nullable = false, length = 10)
    private String statut = "DEMANDE";

    @Column(length = 500)
    private String reponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
