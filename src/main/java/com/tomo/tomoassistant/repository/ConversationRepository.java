package com.tomo.tomoassistant.repository;

import com.tomo.tomoassistant.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findByDeletedFalseOrderByLastMessageAtDesc();
    List<ConversationEntity> findByKnowledgeBaseIdAndDeletedFalseOrderByIdDesc(Long knowledgeBaseId);
}
