package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Classe;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    List<Classe> findByEtablissementIdOrderByNom(Long etablissementId);

    List<Classe> findByEtablissementIdAndAnneeScolaireIdOrderByNom(Long etablissementId, Long anneeScolaireId);

    Optional<Classe> findByEtablissementIdAndAnneeScolaireIdAndNom(Long etablissementId, Long anneeScolaireId,
                                                                   String nom);

    /** Sert au contrôle de périmètre : à quel établissement appartient cette classe ? */
    @Query("select c.etablissement.id from Classe c where c.id = :id")
    Optional<Long> findEtablissementIdById(@Param("id") Long id);

    /** Verrou en écriture : évite de dépasser la capacité si deux acceptations arrivent en même temps. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classe c where c.id = :id")
    Optional<Classe> findByIdPourMiseAJour(@Param("id") Long id);
}
