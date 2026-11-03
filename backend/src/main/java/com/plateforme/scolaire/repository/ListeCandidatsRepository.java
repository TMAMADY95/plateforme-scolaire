package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.ListeCandidats;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeCandidatsRepository extends JpaRepository<ListeCandidats, Long> {

    @org.springframework.data.jpa.repository.Query("select l from ListeCandidats l join fetch l.etablissement where l.examen.id = :examenId")
    List<ListeCandidats> deLExamen(@org.springframework.data.repository.query.Param("examenId") Long examenId);

    /** Listes dans l'un de ces états, avec l'examen et l'établissement (rappels automatiques). */
    @org.springframework.data.jpa.repository.Query("select l from ListeCandidats l join fetch l.examen join fetch l.etablissement where l.statut in :statuts")
    List<ListeCandidats> enEtat(@org.springframework.data.repository.query.Param("statuts") java.util.Collection<String> statuts);

    Optional<ListeCandidats> findByExamenIdAndEtablissementId(Long examenId, Long etablissementId);
}
