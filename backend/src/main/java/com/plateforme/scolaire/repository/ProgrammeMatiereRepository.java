package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ProgrammeMatiere;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgrammeMatiereRepository extends JpaRepository<ProgrammeMatiere, Long> {

    @Query("select p from ProgrammeMatiere p join fetch p.niveau n join fetch p.matiere m where p.anneeScolaire.id = :annee "
            + "order by n.ordre, p.serie, m.libelle")
    List<ProgrammeMatiere> deLAnnee(@Param("annee") Long anneeScolaireId);

    @Query("select p from ProgrammeMatiere p join fetch p.niveau n join fetch p.matiere m where p.anneeScolaire.id = :annee "
            + "and n.id = :niveau order by n.ordre, p.serie, m.libelle")
    List<ProgrammeMatiere> deLAnneeEtNiveau(@Param("annee") Long anneeScolaireId, @Param("niveau") Long niveauId);

    @Query("select p from ProgrammeMatiere p join fetch p.matiere where p.anneeScolaire.id = :annee and p.niveau.id = :niveau "
            + "and (p.serie is null or p.serie = :serie)")
    List<ProgrammeMatiere> pourClasse(@Param("annee") Long anneeScolaireId, @Param("niveau") Long niveauId, @Param("serie") String serie);

    boolean existsByAnneeScolaireId(Long anneeScolaireId);
}
