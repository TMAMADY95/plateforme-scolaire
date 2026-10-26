package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Periode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodeRepository extends JpaRepository<Periode, Long> {
    List<Periode> findByAnneeScolaireIdOrderByOrdre(Long anneeScolaireId);

    boolean existsByAnneeScolaireIdAndOrdre(Long anneeScolaireId, int ordre);
}
