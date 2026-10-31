package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Trace d'une alerte envoyée (ou tentée) : qui a été prévenu, par quel canal, de quoi, et si l'envoi a réussi. */
@Entity
@Table(name = "alerte_envoyee")
@Getter
@Setter
@NoArgsConstructor
public class AlerteEnvoyee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envoyee_le", nullable = false)
    private Instant envoyeeLe;

    /** URGENCE ou PLAN_ACTION */
    @Column(nullable = false, length = 20)
    private String type;

    /** EMAIL ou SMS */
    @Column(nullable = false, length = 10)
    private String canal;

    @Column(nullable = false, length = 255)
    private String destinataire;

    @Column(nullable = false, length = 500)
    private String resume;

    @Column(nullable = false)
    private boolean reussie;
}
