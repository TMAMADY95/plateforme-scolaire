package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une visite d'inspection d'un établissement ; le résultat de la plus récente donne la conformité. */
@Entity
@Table(name = "inspection")
@Getter
@Setter
@NoArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspecteur_id")
    private Utilisateur inspecteur;

    @Column(name = "date_inspection", nullable = false)
    private LocalDate dateInspection;

    /** CONFORME ou NON_CONFORME */
    @Column(nullable = false, length = 20)
    private String resultat;

    @Column(length = 2000)
    private String observations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
