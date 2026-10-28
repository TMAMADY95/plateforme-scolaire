package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.JournalAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    /** Recherche par fragment d'action et d'entité (une chaîne vide les accepte toutes). */
    @Query(value = "select j from JournalAudit j left join fetch j.utilisateur "
            + "where lower(j.action) like lower(concat('%', :action, '%')) "
            + "and lower(j.entite) like lower(concat('%', :entite, '%'))",
            countQuery = "select count(j) from JournalAudit j "
                    + "where lower(j.action) like lower(concat('%', :action, '%')) "
                    + "and lower(j.entite) like lower(concat('%', :entite, '%'))")
    Page<JournalAudit> rechercher(@Param("action") String action, @Param("entite") String entite,
                                  Pageable pageable);
}
