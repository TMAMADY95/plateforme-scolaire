package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ProfilEnseignant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfilEnseignantRepository extends JpaRepository<ProfilEnseignant, Long> {

    List<ProfilEnseignant> findByEnseignantIdIn(Collection<Long> enseignantIds);
}
