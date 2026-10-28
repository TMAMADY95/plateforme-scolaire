package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AffectationRole;
import com.plateforme.scolaire.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AffectationRoleRepository extends JpaRepository<AffectationRole, Long> {
    List<AffectationRole> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurIdAndRole(Long utilisateurId, Role role);

    boolean existsByZoneId(Long zoneId);

    /** Comptes à rôle d'établissement (direction, personnel, enseignants) filtrés par nom, prénom ou e-mail. */
    @Query("select a from AffectationRole a join fetch a.utilisateur u "
            + "where a.etablissement.id = :etablissementId and a.role in :roles "
            + "and (lower(u.nom) like lower(concat('%', :q, '%')) or lower(u.prenom) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "order by u.nom, u.prenom")
    List<AffectationRole> rechercherComptes(@Param("etablissementId") Long etablissementId,
                                            @Param("roles") Collection<Role> roles,
                                            @Param("q") String q);
}
