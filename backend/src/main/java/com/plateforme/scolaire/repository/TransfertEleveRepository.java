package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.TransfertEleve;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransfertEleveRepository extends JpaRepository<TransfertEleve, Long> {

    boolean existsByEleveIdAndAnneeScolaireIdAndStatut(Long eleveId, Long anneeId, String statut);

    List<TransfertEleve> findByEtablissementOrigineIdAndAnneeScolaireIdOrderByDemandeLeDescIdDesc(Long etablissementId, Long anneeId);

    List<TransfertEleve> findByEtablissementDestIdAndAnneeScolaireIdOrderByDemandeLeDescIdDesc(Long etablissementId, Long anneeId);
}
