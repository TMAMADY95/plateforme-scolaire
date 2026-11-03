package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.LivraisonCentre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivraisonCentreRepository extends JpaRepository<LivraisonCentre, Long> {

    List<LivraisonCentre> findByExamenId(Long examenId);

    Optional<LivraisonCentre> findByCentreIdAndArticle(Long centreId, String article);
}
