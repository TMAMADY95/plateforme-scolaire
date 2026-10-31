package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.EpreuveCentre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpreuveCentreRepository extends JpaRepository<EpreuveCentre, Long> {

    List<EpreuveCentre> findByCentreId(Long centreId);

    Optional<EpreuveCentre> findByCentreIdAndEpreuveId(Long centreId, Long epreuveId);
}
