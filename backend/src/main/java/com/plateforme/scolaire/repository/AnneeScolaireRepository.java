package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AnneeScolaire;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnneeScolaireRepository extends JpaRepository<AnneeScolaire, Long> {
    List<AnneeScolaire> findAllByOrderByDateDebutDesc();

    Optional<AnneeScolaire> findByLibelle(String libelle);
}
