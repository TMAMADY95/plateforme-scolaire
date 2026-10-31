package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PersonnelCentre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelCentreRepository extends JpaRepository<PersonnelCentre, Long> {

    List<PersonnelCentre> findByCentreIdOrderByRoleAscSalleAscNomAsc(Long centreId);
}
