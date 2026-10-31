package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AlerteEnvoyee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlerteEnvoyeeRepository extends JpaRepository<AlerteEnvoyee, Long> {

    boolean existsByTypeAndResumeStartingWithAndReussieTrue(String type, String debutResume);

    boolean existsByTypeAndResumeStartingWithAndReussieTrueAndEnvoyeeLeAfter(String type, String debutResume, java.time.Instant apres);

    /** Journal du ministère : sans les alertes qui concernent des élèves et leurs parents (réservées à l'établissement). */
    List<AlerteEnvoyee> findTop200ByTypeNotOrderByEnvoyeeLeDescIdDesc(String type);

    /** SMS d'absence envoyés aux parents des élèves d'un établissement (le résumé commence par « ABS#<id de présence> »). */
    @org.springframework.data.jpa.repository.Query(value = """
            select a.* from alerte_envoyee a
            join presence p on p.id = cast(substring(a.resume from '^ABS#([0-9]+) ') as bigint)
            join inscription i on i.id = p.inscription_id
            where a.type = 'ABSENCE' and a.resume ~ '^ABS#[0-9]+ ' and i.etablissement_id = :etablissementId
            order by a.envoyee_le desc, a.id desc limit 200
            """, nativeQuery = true)
    List<AlerteEnvoyee> absencesDeLEtablissement(@org.springframework.data.repository.query.Param("etablissementId") Long etablissementId);
}
