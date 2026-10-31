package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Inspection;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    @Query("select i from Inspection i join fetch i.inspecteur where i.etablissement.id = :etablissementId "
            + "order by i.dateInspection desc, i.id desc")
    List<Inspection> deLEtablissement(@Param("etablissementId") Long etablissementId);

    /** [établissement, date, résultat], de la plus récente à la plus ancienne. */
    @Query("select i.etablissement.id, i.dateInspection, i.resultat from Inspection i "
            + "where i.etablissement.id in :ids order by i.dateInspection desc, i.id desc")
    List<Object[]> resumes(@Param("ids") Collection<Long> ids);
}
