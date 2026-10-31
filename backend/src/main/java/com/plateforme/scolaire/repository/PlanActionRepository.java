package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PlanAction;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanActionRepository extends JpaRepository<PlanAction, Long> {

    @Query("select p from PlanAction p join fetch p.creePar left join fetch p.etablissement e left join fetch e.zone "
            + "where e.id = :etablissementId order by p.echeance, p.id")
    List<PlanAction> deLEtablissement(@Param("etablissementId") Long etablissementId);

    @Query("select p from PlanAction p join fetch p.creePar join fetch p.etablissement e left join fetch e.zone "
            + "where e.id in :ids order by p.echeance, p.id")
    List<PlanAction> dansLePerimetre(@Param("ids") Collection<Long> ids);

    /** Plans non terminés dont l'échéance est proche ou dépassée et qui n'ont pas été rappelés récemment. */
    @Query("select p from PlanAction p join fetch p.etablissement where p.statut in ('A_FAIRE', 'EN_COURS') "
            + "and p.echeance <= :limite and (p.dernierRappel is null or p.dernierRappel < :avant)")
    List<PlanAction> aRappeler(@Param("limite") LocalDate limite, @Param("avant") Instant avant);
}
