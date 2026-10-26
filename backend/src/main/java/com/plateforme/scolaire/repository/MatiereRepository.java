package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Matiere;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {
    List<Matiere> findAllByOrderByLibelle();

    Optional<Matiere> findByCode(String code);
}
