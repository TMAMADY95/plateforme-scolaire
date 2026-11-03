package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PersonnelExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelExamenRepository extends JpaRepository<PersonnelExamen, Long> {

    List<PersonnelExamen> findByExamenIdOrderByFonctionAscNomAsc(Long examenId);

    boolean existsByExamenIdAndFonctionAndNomIgnoreCase(Long examenId, String fonction, String nom);
}
