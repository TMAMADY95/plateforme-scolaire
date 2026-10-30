package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Annonce;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    @Query("select a from Annonce a join fetch a.auteur where a.classe.id = :classeId "
            + "order by a.createdAt desc, a.id desc")
    List<Annonce> recentes(@Param("classeId") Long classeId, Pageable page);
}
