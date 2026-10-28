package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Exoneration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExonerationRepository extends JpaRepository<Exoneration, Long> {

    List<Exoneration> findByFactureIdOrderByCreatedAtDesc(Long factureId);
}
