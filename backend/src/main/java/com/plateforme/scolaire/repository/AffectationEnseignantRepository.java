package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffectationEnseignantRepository extends JpaRepository<AffectationEnseignant, Long> {

    List<AffectationEnseignant> findByClasseId(Long classeId);

    /** Toutes les classes et matières d'un enseignant, identifié par son compte utilisateur. */
    List<AffectationEnseignant> findByEnseignantUtilisateurId(Long utilisateurId);

    boolean existsByEnseignantIdAndClasseIdAndMatiereId(Long enseignantId, Long classeId, Long matiereId);

    /** Cet utilisateur enseigne-t-il dans cette classe ? */
    boolean existsByClasseIdAndEnseignantUtilisateurId(Long classeId, Long utilisateurId);

    /** Cet utilisateur enseigne-t-il cette matière dans cette classe ? */
    boolean existsByClasseIdAndMatiereIdAndEnseignantUtilisateurId(Long classeId, Long matiereId, Long utilisateurId);
}
