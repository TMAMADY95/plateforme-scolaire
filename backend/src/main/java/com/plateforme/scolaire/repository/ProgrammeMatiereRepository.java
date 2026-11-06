package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ProgrammeMatiere;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgrammeMatiereRepository extends JpaRepository<ProgrammeMatiere, Long> {

    @Query("select p from ProgrammeMatiere p join fetch p.niveau n left join fetch n.cycle join fetch p.matiere m left join fetch p.typeEtablissement where p.anneeScolaire.id = :annee "
            + "order by n.ordre, p.serie, m.libelle")
    List<ProgrammeMatiere> deLAnnee(@Param("annee") Long anneeScolaireId);

    @Query("select p from ProgrammeMatiere p join fetch p.niveau n left join fetch n.cycle join fetch p.matiere m left join fetch p.typeEtablissement where p.anneeScolaire.id = :annee "
            + "and n.id = :niveau order by n.ordre, p.serie, m.libelle")
    List<ProgrammeMatiere> deLAnneeEtNiveau(@Param("annee") Long anneeScolaireId, @Param("niveau") Long niveauId);

    /** Lignes applicables à une classe : celles du niveau, de sa série (ou de toutes les séries) et de son type d'établissement (ou de tous les types). */
    @Query("select p from ProgrammeMatiere p join fetch p.matiere left join fetch p.typeEtablissement where p.anneeScolaire.id = :annee and p.niveau.id = :niveau "
            + "and (p.serie is null or p.serie = :serie) and (p.typeEtablissement is null or p.typeEtablissement.id = :type)")
    List<ProgrammeMatiere> pourClasse(@Param("annee") Long anneeScolaireId, @Param("niveau") Long niveauId, @Param("serie") String serie,
            @Param("type") Long typeEtablissementId);

    boolean existsByAnneeScolaireId(Long anneeScolaireId);
}
