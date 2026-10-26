package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Etablissement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {

    /** Établissements d'une zone et de toutes ses sous-zones (requête récursive). */
    @Query(value = """
            WITH RECURSIVE sous_zones AS (
                SELECT id FROM zone_geographique WHERE id = :zoneId
                UNION ALL
                SELECT z.id FROM zone_geographique z JOIN sous_zones s ON z.parent_id = s.id
            )
            SELECT e.* FROM etablissement e
            WHERE e.zone_id IN (SELECT id FROM sous_zones)
            ORDER BY e.nom
            """, nativeQuery = true)
    List<Etablissement> findByZoneEtSousZones(@Param("zoneId") Long zoneId);
}
