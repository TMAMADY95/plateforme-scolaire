package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dernier état connu d'un établissement pour les alertes d'inspection urgente. */
@Entity
@Table(name = "alerte_urgence_etat")
@Getter
@Setter
@NoArgsConstructor
public class AlerteUrgenceEtat {

    @Id
    @Column(name = "etablissement_id")
    private Long etablissementId;

    @Column(nullable = false)
    private boolean urgent;

    @Column(name = "derniere_alerte")
    private Instant derniereAlerte;

    public AlerteUrgenceEtat(Long etablissementId) {
        this.etablissementId = etablissementId;
    }
}
