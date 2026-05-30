package com.tomo.tomoassistant.repository;

import com.tomo.tomoassistant.model.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByKnowledgeBaseIdAndDeletedFalseOrderByIdDesc(Long knowledgeBaseId);
    List<DocumentEntity> findByStatus(Integer status);
    long countByKnowledgeBaseIdAndStatusAndDeletedFalse(Long knowledgeBaseId, Integer status);
}
