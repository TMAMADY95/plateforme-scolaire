package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Devoir;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevoirRepository extends JpaRepository<Devoir, Long> {

    List<Devoir> findByClasseIdAndDateLimiteGreaterThanEqualOrderByDateLimiteAscIdAsc(Long classeId, LocalDate depuis);
}
