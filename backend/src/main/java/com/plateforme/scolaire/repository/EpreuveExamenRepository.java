package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.EpreuveExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpreuveExamenRepository extends JpaRepository<EpreuveExamen, Long> {

    List<EpreuveExamen> findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(Long examenId);
}
