package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Conversation;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("select c from Conversation c join fetch c.eleve join fetch c.parent join fetch c.personnel "
            + "where c.parent.id = :u or c.personnel.id = :u order by c.dernierMessageLe desc, c.id desc")
    List<Conversation> deLUtilisateur(@Param("u") Long utilisateurId, Pageable page);
}
