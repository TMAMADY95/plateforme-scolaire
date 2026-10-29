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

    /**
     * Même recherche, limitée aux événements qui concernent l'un des élèves : inscriptions, factures, paiements,
     * corrections de présence et notes. Une note supprimée n'existe plus : son événement n'est alors plus rattaché.
     */
    @Query(value = "select j from JournalAudit j left join fetch j.utilisateur "
            + "where lower(j.action) like lower(concat('%', :action, '%')) "
            + "and lower(j.entite) like lower(concat('%', :entite, '%')) "
            + "and ((lower(j.entite) = 'inscription' and j.entiteId in "
            + "(select i.id from Inscription i where i.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'facture' and j.entiteId in "
            + "(select f.id from Facture f where f.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'paiement' and j.entiteId in "
            + "(select p.id from Paiement p where p.facture.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'presence' and j.entiteId in "
            + "(select pr.id from Presence pr where pr.inscription.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'note' and j.entiteId in "
            + "(select n.id from Note n where n.inscription.eleve.id in :eleves)))",
            countQuery = "select count(j) from JournalAudit j "
            + "where lower(j.action) like lower(concat('%', :action, '%')) "
            + "and lower(j.entite) like lower(concat('%', :entite, '%')) "
            + "and ((lower(j.entite) = 'inscription' and j.entiteId in "
            + "(select i.id from Inscription i where i.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'facture' and j.entiteId in "
            + "(select f.id from Facture f where f.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'paiement' and j.entiteId in "
            + "(select p.id from Paiement p where p.facture.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'presence' and j.entiteId in "
            + "(select pr.id from Presence pr where pr.inscription.eleve.id in :eleves)) "
            + "or (lower(j.entite) = 'note' and j.entiteId in "
            + "(select n.id from Note n where n.inscription.eleve.id in :eleves)))")
    Page<JournalAudit> rechercherPourEleves(@Param("action") String action, @Param("entite") String entite,
                                            @Param("eleves") java.util.Collection<Long> eleves, Pageable pageable);
}
