package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ProfilVulnerabiliteEleve;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfilVulnerabiliteEleveRepository extends JpaRepository<ProfilVulnerabiliteEleve, Long> {

    List<ProfilVulnerabiliteEleve> findByEleveIdIn(Collection<Long> eleveIds);
}
