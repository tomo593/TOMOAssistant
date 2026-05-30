package com.tomo.tomoassistant.repository;

import com.tomo.tomoassistant.model.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {
    List<KnowledgeBaseEntity> findByDeletedFalseOrderByIdDesc();
    List<KnowledgeBaseEntity> findByNameContainingAndDeletedFalse(String name);
}
