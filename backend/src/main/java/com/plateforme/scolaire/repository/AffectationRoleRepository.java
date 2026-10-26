package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffectationRoleRepository extends JpaRepository<AffectationRole, Long> {
    List<AffectationRole> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurIdAndRole(Long utilisateurId, Role role);
}
