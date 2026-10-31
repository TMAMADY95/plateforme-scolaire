package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CandidatExamen;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidatExamenRepository extends JpaRepository<CandidatExamen, Long> {

    @Query("select c from CandidatExamen c join fetch c.eleve left join fetch c.classe left join fetch c.centre "
            + "where c.examen.id = :examenId and c.etablissement.id = :etablissementId order by c.eleve.nom, c.eleve.prenom")
    List<CandidatExamen> deLEtablissement(@Param("examenId") Long examenId, @Param("etablissementId") Long etablissementId);

    @Query("select c.eleve.id from CandidatExamen c where c.examen.id = :examenId and c.eleve.id in :eleveIds")
    Set<Long> elevesDejaInscrits(@Param("examenId") Long examenId, @Param("eleveIds") Collection<Long> eleveIds);

    long countByExamenIdAndEtablissementId(Long examenId, Long etablissementId);

    /** Candidats de l'examen qui n'ont pas encore de centre. */
    @Query("select c from CandidatExamen c join fetch c.etablissement where c.examen.id = :examenId and c.centre is null")
    List<CandidatExamen> sansCentre(@Param("examenId") Long examenId);

    long countByCentreId(Long centreId);

    /** Candidats d'un centre, dans l'ordre de la répartition (établissement, puis nom). */
    @Query("select c from CandidatExamen c join fetch c.eleve join fetch c.etablissement "
            + "where c.centre.id = :centreId order by c.etablissement.nom, c.eleve.nom, c.eleve.prenom, c.id")
    List<CandidatExamen> duCentre(@Param("centreId") Long centreId);

    /** Candidats dont les épreuves ont commencé avant [limite] et dont l'établissement n'a transmis aucun résultat. */
    @Query("select c from CandidatExamen c join fetch c.examen join fetch c.etablissement "
            + "where c.centre.dateDebut < :limite and not exists (select r.id from ResultatExamen r "
            + "where r.examen.id = c.examen.id and r.etablissement.id = c.etablissement.id)")
    List<CandidatExamen> sansResultats(@Param("limite") java.time.LocalDate limite);
}
