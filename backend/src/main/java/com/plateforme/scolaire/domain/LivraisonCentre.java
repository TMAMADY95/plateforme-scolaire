package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une fourniture d'examen (sujets, copies, enveloppes, fournitures) à livrer à un centre : prévue, expédiée, reçue. */
@Entity
@Table(name = "livraison_centre")
@Getter
@Setter
@NoArgsConstructor
public class LivraisonCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id")
    private CentreExamen centre;

    @Column(nullable = false, length = 20)
    private String article;

    @Column(name = "quantite_prevue", nullable = false)
    private int quantitePrevue;

    @Column(name = "quantite_livree")
    private Integer quantiteLivree;

    /** PREVUE, EXPEDIEE ou RECUE. */
    @Column(nullable = false, length = 10)
    private String statut = "PREVUE";

    @Column(name = "expediee_le")
    private Instant expedieeLe;

    @Column(name = "recue_le")
    private Instant recueLe;

    @Column(length = 500)
    private String observation;
}
