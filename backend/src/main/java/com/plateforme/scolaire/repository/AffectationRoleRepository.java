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

    java.util.Optional<AffectationRole> findByUtilisateurIdAndRoleAndEtablissementId(Long utilisateurId, Role role, Long etablissementId);

    /** Comptes à rôle d'établissement (direction, personnel, enseignants) filtrés par nom, prénom ou e-mail. */
    @Query("select a from AffectationRole a join fetch a.utilisateur u "
            + "where a.etablissement.id = :etablissementId and a.role in :roles "
            + "and (lower(u.nom) like lower(concat('%', :q, '%')) or lower(u.prenom) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "order by u.nom, u.prenom")
    List<AffectationRole> rechercherComptes(@Param("etablissementId") Long etablissementId,
                                            @Param("roles") Collection<Role> roles,
                                            @Param("q") String q);

    /** Affectations des comptes actifs à ces rôles, avec le compte et la zone : destinataires des alertes. */
    @Query("select a from AffectationRole a join fetch a.utilisateur u left join fetch a.zone "
            + "where a.role in :roles and u.actif = true")
    List<AffectationRole> destinataires(@Param("roles") Collection<Role> roles);

    /** Toutes les affectations à ces rôles (comptes actifs ou non), avec compte et zone. */
    @Query("select a from AffectationRole a join fetch a.utilisateur left join fetch a.zone where a.role in :roles")
    List<AffectationRole> destinatairesTous(@Param("roles") Collection<Role> roles);

    /** Les comptes actifs de direction de ces établissements. */
    @Query("select a from AffectationRole a join fetch a.utilisateur u where u.actif = true "
            + "and a.role = com.plateforme.scolaire.domain.Role.DIRECTION_ETABLISSEMENT and a.etablissement.id in :ids")
    List<AffectationRole> directionsDe(@Param("ids") Collection<Long> ids);
}
