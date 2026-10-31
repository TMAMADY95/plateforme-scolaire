package com.plateforme.scolaire.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Candidats présentés et admis d'un établissement à un examen national. */
@Entity
@Table(name = "resultat_examen")
@Getter
@Setter
@NoArgsConstructor
public class ResultatExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id")
    private ExamenNational examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(nullable = false)
    private int presentes;

    @Column(nullable = false)
    private int admis;

    /** Candidats inscrits qui ne se sont pas présentés, et candidats éliminés pour fraude. */
    @Column(nullable = false)
    private int absents;

    @Column(nullable = false)
    private int elimines;

    /** Filles présentes et admises ; null quand la répartition par sexe n'est pas renseignée. */
    @Column(name = "presentes_filles")
    private Integer presentesFilles;

    @Column(name = "admis_filles")
    private Integer admisFilles;

    /** Baccalauréat : résultats par série (sciences mathématiques, sciences sociales, sciences expérimentales). */
    @ElementCollection
    @CollectionTable(name = "resultat_examen_serie", joinColumns = @JoinColumn(name = "resultat_id"))
    @org.hibernate.annotations.BatchSize(size = 100)
    private java.util.List<SerieResultat> series = new java.util.ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SerieResultat {
        @Column(nullable = false, length = 30)
        private String serie;
        @Column(nullable = false)
        private int presentes;
        @Column(nullable = false)
        private int admis;

        public SerieResultat(String serie, int presentes, int admis) {
            this.serie = serie;
            this.presentes = presentes;
            this.admis = admis;
        }
    }
}
