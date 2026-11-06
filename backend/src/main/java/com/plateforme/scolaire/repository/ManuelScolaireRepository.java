package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ManuelScolaire;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManuelScolaireRepository extends JpaRepository<ManuelScolaire, Long> {

    List<ManuelScolaire> findAllByOrderByNiveauOrdreAscMatiereLibelleAscTitreAsc();

    boolean existsByMatiereIdAndNiveauIdAndTitreIgnoreCase(Long matiereId, Long niveauId, String titre);

    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from ManuelScolaire m where m.matiere.id = :matiere and m.niveau.id = :niveau "
            + "and ((:serie is null and m.serie is null) or m.serie = :serie) and lower(m.titre) = lower(:titre)")
    boolean existe(@org.springframework.data.repository.query.Param("matiere") Long matiereId, @org.springframework.data.repository.query.Param("niveau") Long niveauId,
            @org.springframework.data.repository.query.Param("serie") String serie, @org.springframework.data.repository.query.Param("titre") String titre);
}
