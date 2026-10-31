package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.TypeEtablissement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeEtablissementRepository extends JpaRepository<TypeEtablissement, Long> {

    List<TypeEtablissement> findAllByOrderByLibelle();

    boolean existsByCodeIgnoreCase(String code);
}
