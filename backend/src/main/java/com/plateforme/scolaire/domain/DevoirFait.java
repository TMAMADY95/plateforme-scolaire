package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un élève a terminé ce devoir (coché par lui ou par l'un de ses parents). */
@Entity
@Table(name = "devoir_fait")
@Getter
@Setter
@NoArgsConstructor
public class DevoirFait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devoir_id")
    private Devoir devoir;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @Column(name = "fait_le", nullable = false)
    private Instant faitLe;

    @Column(name = "par_utilisateur_id", nullable = false)
    private Long parUtilisateurId;
}
