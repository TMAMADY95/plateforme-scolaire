package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.IncidentExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentExamenRepository extends JpaRepository<IncidentExamen, Long> {

    @Query("select i from IncidentExamen i left join fetch i.candidat c left join fetch c.eleve left join fetch i.epreuve "
            + "where i.centre.id = :centreId order by i.survenuLe desc, i.id desc")
    List<IncidentExamen> duCentre(@Param("centreId") Long centreId);
}
