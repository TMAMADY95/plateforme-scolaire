package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CycleScolaire;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleScolaireRepository extends JpaRepository<CycleScolaire, Long> {

    List<CycleScolaire> findAllByOrderByOrdre();

    Optional<CycleScolaire> findByCode(String code);
}
