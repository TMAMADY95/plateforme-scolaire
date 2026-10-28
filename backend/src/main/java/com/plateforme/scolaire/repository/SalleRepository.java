package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Salle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    List<Salle> findByEtablissementIdOrderByNom(Long etablissementId);

    Optional<Salle> findByEtablissementIdAndNom(Long etablissementId, String nom);

    boolean existsByEtablissementIdAndNom(Long etablissementId, String nom);
}
