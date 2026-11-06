package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.FicheInfrastructure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FicheInfrastructureRepository extends JpaRepository<FicheInfrastructure, Long> {

    Optional<FicheInfrastructure> findByEtablissementIdAndAnneeScolaireId(Long etablissementId, Long anneeScolaireId);

    List<FicheInfrastructure> findByAnneeScolaireId(Long anneeScolaireId);
}
