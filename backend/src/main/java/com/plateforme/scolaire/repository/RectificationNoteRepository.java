package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.RectificationNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RectificationNoteRepository extends JpaRepository<RectificationNote, Long> {

    /** Rectifications non annulées d'un candidat, la plus récente d'abord. */
    @Query("select r from RectificationNote r where r.candidat.id = :candidatId and r.annuleeLe is null order by r.rectifieeLe desc, r.id desc")
    List<RectificationNote> activesDuCandidat(@Param("candidatId") Long candidatId);

    @Query("select r from RectificationNote r where r.reclamation.id = :reclamationId and r.annuleeLe is null order by r.id desc")
    List<RectificationNote> activesDeLaReclamation(@Param("reclamationId") Long reclamationId);
}
