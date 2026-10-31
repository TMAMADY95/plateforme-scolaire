package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Demande de justification d'une absence ou d'un retard, faite en ligne par un parent ou l'élève. */
@Entity
@Table(name = "justification_absence")
@Getter
@Setter
@NoArgsConstructor
public class JustificationAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presence_id")
    private Presence presence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soumise_par_id")
    private Utilisateur soumisePar;

    @Column(nullable = false, length = 500)
    private String motif;

    /** EN_ATTENTE, ACCEPTEE ou REFUSEE */
    @Column(nullable = false, length = 10)
    private String statut = "EN_ATTENTE";

    @Column(length = 500)
    private String reponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traitee_par_id")
    private Utilisateur traiteePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "traitee_le")
    private Instant traiteeLe;
}
