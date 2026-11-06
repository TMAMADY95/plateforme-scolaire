package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.DecisionFinAnnee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionFinAnneeRepository extends JpaRepository<DecisionFinAnnee, Long> {

    List<DecisionFinAnnee> findByClasseId(Long classeId);

    Optional<DecisionFinAnnee> findByInscriptionId(Long inscriptionId);

    List<DecisionFinAnnee> findByInscriptionIdIn(java.util.Collection<Long> inscriptionIds);
}
