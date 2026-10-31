package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PieceCandidature;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PieceCandidatureRepository extends JpaRepository<PieceCandidature, Long> {

    List<PieceCandidature> findByCandidatIdIn(Collection<Long> candidatIds);

    Optional<PieceCandidature> findByCandidatIdAndType(Long candidatId, String type);
}
