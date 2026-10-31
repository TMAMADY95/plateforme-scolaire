package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ListeCandidats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeCandidatsRepository extends JpaRepository<ListeCandidats, Long> {

    Optional<ListeCandidats> findByExamenIdAndEtablissementId(Long examenId, Long etablissementId);
}
