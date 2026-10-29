package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m join fetch m.auteur where m.conversation.id = :id order by m.createdAt, m.id")
    List<Message> deLaConversation(@Param("id") Long conversationId);
}
