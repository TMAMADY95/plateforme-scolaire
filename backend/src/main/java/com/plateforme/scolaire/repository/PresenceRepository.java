package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Presence;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findByInscriptionClasseIdAndDateSeance(Long classeId, LocalDate dateSeance);

    List<Presence> findByInscriptionEleveIdAndDateSeanceBetweenOrderByDateSeanceDesc(
            Long eleveId, LocalDate du, LocalDate au);
}
