package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.DotationManuel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DotationManuelRepository extends JpaRepository<DotationManuel, Long> {

    Optional<DotationManuel> findByEtablissementIdAndAnneeScolaireIdAndManuelId(Long etablissementId, Long anneeId, Long manuelId);

    List<DotationManuel> findByEtablissementIdAndAnneeScolaireId(Long etablissementId, Long anneeId);

    List<DotationManuel> findByAnneeScolaireId(Long anneeId);

    boolean existsByManuelId(Long manuelId);
}
