package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.JustificationAbsence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JustificationAbsenceRepository extends JpaRepository<JustificationAbsence, Long> {

    boolean existsByPresenceIdAndStatut(Long presenceId, String statut);

    @Query("select j from JustificationAbsence j join fetch j.presence p join fetch p.inscription i join fetch i.eleve "
            + "left join fetch i.classe join fetch j.soumisePar where i.eleve.id = :eleveId order by j.createdAt desc")
    List<JustificationAbsence> deLEleve(@Param("eleveId") Long eleveId);

    @Query("select j from JustificationAbsence j join fetch j.presence p join fetch p.inscription i join fetch i.eleve "
            + "left join fetch i.classe join fetch j.soumisePar where i.etablissement.id = :etablissementId "
            + "and j.statut = 'EN_ATTENTE' order by j.createdAt")
    List<JustificationAbsence> enAttenteDeLEtablissement(@Param("etablissementId") Long etablissementId);
}
