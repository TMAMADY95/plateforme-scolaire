package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AbandonScolaire;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbandonScolaireRepository extends JpaRepository<AbandonScolaire, Long> {

    List<AbandonScolaire> findByEtablissementIdAndAnneeScolaireId(Long etablissementId, Long anneeScolaireId);

    boolean existsByInscriptionId(Long inscriptionId);
}
