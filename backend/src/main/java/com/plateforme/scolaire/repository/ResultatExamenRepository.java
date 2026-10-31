package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ResultatExamen;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultatExamenRepository extends JpaRepository<ResultatExamen, Long> {

    Optional<ResultatExamen> findByExamenIdAndEtablissementId(Long examenId, Long etablissementId);

    long countByExamenId(Long examenId);

    @Query("select r from ResultatExamen r join fetch r.etablissement where r.examen.id = :examenId and r.etablissement.id in :ids")
    List<ResultatExamen> deLExamen(@Param("examenId") Long examenId, @Param("ids") Collection<Long> ids);

    /** [établissement, présentés, admis] pour un examen. */
    @Query("select r.etablissement.id, r.presentes, r.admis from ResultatExamen r "
            + "where r.examen.id = :examenId and r.etablissement.id in :ids")
    List<Object[]> chiffres(@Param("examenId") Long examenId, @Param("ids") Collection<Long> ids);

    /** [année scolaire, présentés, admis] tous examens confondus pour ces établissements. */
    @Query("select e.anneeScolaire.id, sum(r.presentes), sum(r.admis) from ResultatExamen r join r.examen e "
            + "where r.etablissement.id in :ids group by e.anneeScolaire.id")
    List<Object[]> totauxParAnnee(@Param("ids") Collection<Long> ids);
}
