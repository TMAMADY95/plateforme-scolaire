package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Creneau;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    List<Creneau> findByClasseIdOrderByJourSemaineAscHeureDebutAsc(Long classeId);

    /** Une salle utilisée par au moins un créneau ne peut pas être supprimée. */
    boolean existsBySalleId(Long salleId);

    List<Creneau> findByEnseignantIdOrderByJourSemaineAscHeureDebutAsc(Long enseignantId);

    /** Créneaux de toutes les classes d'un établissement pour une année scolaire. */
    @Query("select c from Creneau c where c.classe.etablissement.id = :etablissementId "
            + "and c.classe.anneeScolaire.id = :anneeScolaireId "
            + "order by c.classe.nom, c.jourSemaine, c.heureDebut")
    List<Creneau> findByEtablissementEtAnnee(@Param("etablissementId") Long etablissementId,
                                             @Param("anneeScolaireId") Long anneeScolaireId);

    /** Sert au contrôle de périmètre : à quelle classe appartient ce créneau ? */
    @Query("select c.classe.id from Creneau c where c.id = :id")
    Optional<Long> findClasseIdById(@Param("id") Long id);

    /**
     * Chevauchement pour un enseignant : un autre créneau du même jour dont l'intervalle
     * recoupe [debut, fin). Sert au contrôle métier avant l'insertion.
     */
    @Query("select c from Creneau c where c.enseignant.id = :enseignantId and c.jourSemaine = :jour "
            + "and c.heureDebut < :fin and c.heureFin > :debut "
            + "and (:exclureId is null or c.id <> :exclureId)")
    List<Creneau> chevauchementsEnseignant(@Param("enseignantId") Long enseignantId,
                                           @Param("jour") Short jour,
                                           @Param("debut") LocalTime debut,
                                           @Param("fin") LocalTime fin,
                                           @Param("exclureId") Long exclureId);

    /** Chevauchement pour une classe, sur le même principe. */
    @Query("select c from Creneau c where c.classe.id = :classeId and c.jourSemaine = :jour "
            + "and c.heureDebut < :fin and c.heureFin > :debut "
            + "and (:exclureId is null or c.id <> :exclureId)")
    List<Creneau> chevauchementsClasse(@Param("classeId") Long classeId,
                                       @Param("jour") Short jour,
                                       @Param("debut") LocalTime debut,
                                       @Param("fin") LocalTime fin,
                                       @Param("exclureId") Long exclureId);

    /** Chevauchement pour une salle, sur le même principe. */
    @Query("select c from Creneau c where c.salle.id = :salleId and c.jourSemaine = :jour "
            + "and c.heureDebut < :fin and c.heureFin > :debut "
            + "and (:exclureId is null or c.id <> :exclureId)")
    List<Creneau> chevauchementsSalle(@Param("salleId") Long salleId,
                                      @Param("jour") Short jour,
                                      @Param("debut") LocalTime debut,
                                      @Param("fin") LocalTime fin,
                                      @Param("exclureId") Long exclureId);
}
