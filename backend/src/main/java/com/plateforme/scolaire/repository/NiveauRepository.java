package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Niveau;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NiveauRepository extends JpaRepository<Niveau, Long> {
    List<Niveau> findAllByOrderByOrdre();

    Optional<Niveau> findByCode(String code);
}
