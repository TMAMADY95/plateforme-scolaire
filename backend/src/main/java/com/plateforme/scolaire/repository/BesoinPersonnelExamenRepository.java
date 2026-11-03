package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.BesoinPersonnelExamen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BesoinPersonnelExamenRepository extends JpaRepository<BesoinPersonnelExamen, Long> {

    List<BesoinPersonnelExamen> findByExamenId(Long examenId);

    Optional<BesoinPersonnelExamen> findByExamenIdAndFonction(Long examenId, String fonction);
}
