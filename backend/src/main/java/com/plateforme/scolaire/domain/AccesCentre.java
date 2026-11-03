package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un compte de chef de centre et le centre d'examen qui lui est confié. */
@Entity
@Table(name = "acces_centre")
@Getter
@Setter
@NoArgsConstructor
public class AccesCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;
}
