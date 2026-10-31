package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CahierTexte;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CahierTexteRepository extends JpaRepository<CahierTexte, Long> {

    List<CahierTexte> findByClasseIdAndDateSeanceBetweenOrderByDateSeanceDescIdDesc(Long classeId, LocalDate du, LocalDate au);
}
