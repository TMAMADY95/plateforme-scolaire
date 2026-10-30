package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.DevoirFait;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DevoirFaitRepository extends JpaRepository<DevoirFait, Long> {

    Optional<DevoirFait> findByDevoirIdAndEleveId(Long devoirId, Long eleveId);

    /** Parmi ces devoirs, ceux que l'élève a terminés. [ids] ne doit pas être vide. */
    @Query("select f.devoir.id from DevoirFait f where f.eleve.id = :eleveId and f.devoir.id in :ids")
    List<Long> idsTermines(@Param("eleveId") Long eleveId, @Param("ids") Collection<Long> ids);

    /** Pour chaque devoir : [id du devoir, nombre d'élèves l'ayant terminé]. [ids] ne doit pas être vide. */
    @Query("select f.devoir.id, count(f) from DevoirFait f where f.devoir.id in :ids group by f.devoir.id")
    List<Object[]> compter(@Param("ids") Collection<Long> ids);
}
