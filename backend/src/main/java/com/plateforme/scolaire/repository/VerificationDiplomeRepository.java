package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.VerificationDiplome;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationDiplomeRepository extends JpaRepository<VerificationDiplome, Long> {

    long countByResultat(String resultat);

    long countByVerifieLeAfter(Instant depuis);

    /** Codes inconnus saisis au moins {@code min} fois depuis la date donnée : [code, tentatives, dernière tentative]. */
    @Query("select v.codeSaisi, count(v.id), max(v.verifieLe) from VerificationDiplome v where v.resultat = 'INCONNU' and v.verifieLe >= :depuis "
            + "group by v.codeSaisi having count(v.id) >= :min order by count(v.id) desc, max(v.verifieLe) desc")
    List<Object[]> inconnusRepetes(@Param("depuis") Instant depuis, @Param("min") long min);
}
