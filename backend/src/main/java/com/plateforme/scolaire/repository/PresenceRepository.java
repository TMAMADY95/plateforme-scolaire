package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Presence;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findByInscriptionClasseIdAndDateSeance(Long classeId, LocalDate dateSeance);

    /** Lignes {inscription, séances saisies, absences} pour les inscriptions données. */
    @Query("select p.inscription.id, count(p), sum(case when p.statut = 'ABSENT' then 1 else 0 end) "
            + "from Presence p where p.inscription.id in :ids group by p.inscription.id")
    List<Object[]> absencesParInscription(@Param("ids") Collection<Long> ids);

    List<Presence> findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(
            Long eleveId, LocalDate du, LocalDate au);
}
