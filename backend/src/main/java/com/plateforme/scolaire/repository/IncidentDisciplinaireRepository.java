package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.IncidentDisciplinaire;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentDisciplinaireRepository extends JpaRepository<IncidentDisciplinaire, Long> {

    List<IncidentDisciplinaire> findByEtablissementIdAndAnneeScolaireIdOrderByDateFaitsDescIdDesc(Long etablissementId, Long anneeId);

    List<IncidentDisciplinaire> findByEleveIdOrderByDateFaitsDescIdDesc(Long eleveId);
}
