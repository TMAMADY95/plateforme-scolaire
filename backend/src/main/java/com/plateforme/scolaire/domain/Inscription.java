package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "inscription")
@Getter
@Setter
@NoArgsConstructor
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id")
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    /** Renseignée au moment de la décision. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    /** EN_ATTENTE, ACCEPTEE, REFUSEE, ANNULEE ou TRANSFEREE */
    @Column(nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande = LocalDate.now();

    @Column(name = "date_decision")
    private LocalDate dateDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decide_par")
    private Utilisateur decidePar;
}
