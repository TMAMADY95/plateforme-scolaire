package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.DiplomeExamen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiplomeExamenRepository extends JpaRepository<DiplomeExamen, Long> {

    boolean existsByCode(String code);

    @Query("select d from DiplomeExamen d join fetch d.candidat c join fetch c.eleve join fetch c.etablissement "
            + "where c.examen.id = :examenId order by c.etablissement.nom, c.eleve.nom, c.eleve.prenom")
    List<DiplomeExamen> deLExamen(@Param("examenId") Long examenId);

    @Query("select d from DiplomeExamen d join fetch d.candidat c join fetch c.eleve join fetch c.etablissement "
            + "where c.examen.id = :examenId and c.etablissement.id = :etablissementId and d.revoque = false order by c.eleve.nom, c.eleve.prenom")
    List<DiplomeExamen> valablesDeLEtablissement(@Param("examenId") Long examenId, @Param("etablissementId") Long etablissementId);

    @Query("select d from DiplomeExamen d join fetch d.candidat c join fetch c.eleve join fetch c.examen e join fetch e.anneeScolaire where d.code = :code")
    Optional<DiplomeExamen> parCode(@Param("code") String code);

    /** Candidats admis qui n'ont pas encore leur diplôme. */
    @Query("select c from CandidatExamen c join fetch c.eleve where c.examen.id = :examenId and c.decision = 'ADMIS' "
            + "and not exists (select 1 from DiplomeExamen d where d.candidat = c)")
    List<CandidatExamen> admisSansDiplome(@Param("examenId") Long examenId);

    @Query("select count(c) from CandidatExamen c where c.examen.id = :examenId and c.decision = 'ADMIS'")
    long countAdmis(@Param("examenId") Long examenId);
}
