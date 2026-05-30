package com.tomo.tomoassistant.repository;

import com.tomo.tomoassistant.model.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<MessageEntity> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
