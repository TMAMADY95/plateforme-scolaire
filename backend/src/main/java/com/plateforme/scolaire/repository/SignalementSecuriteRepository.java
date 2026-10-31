package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.SignalementSecurite;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SignalementSecuriteRepository extends JpaRepository<SignalementSecurite, Long> {

    @Query("select s from SignalementSecurite s join fetch s.signalePar where s.etablissement.id = :etablissementId "
            + "order by s.statut desc, s.createdAt desc")
    List<SignalementSecurite> deLEtablissement(@Param("etablissementId") Long etablissementId);

    /** [établissement, gravité, nombre de signalements ouverts]. */
    @Query("select s.etablissement.id, s.gravite, count(s) from SignalementSecurite s "
            + "where s.etablissement.id in :ids and s.statut = 'OUVERT' group by s.etablissement.id, s.gravite")
    List<Object[]> ouverts(@Param("ids") Collection<Long> ids);
}
