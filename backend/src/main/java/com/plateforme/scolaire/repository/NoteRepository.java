package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Note;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByEvaluationId(Long evaluationId);

    @Query("select max(n.valeur) from Note n where n.evaluation.id = :evaluationId")
    BigDecimal maxValeur(@Param("evaluationId") Long evaluationId);

    /** Notes chiffrées (hors absences) d'une classe pour une période, avec évaluation et matière chargées. */
    @Query("select n from Note n join fetch n.evaluation e join fetch e.matiere "
            + "where e.classe.id = :classeId and e.periode.id = :periodeId "
            + "and n.absent = false and n.valeur is not null")
    List<Note> findPourCalcul(@Param("classeId") Long classeId, @Param("periodeId") Long periodeId);
}
