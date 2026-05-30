package com.tomo.tomoassistant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@Table(name = "knowledge_base")
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE knowledge_base SET deleted = 1 WHERE id = ?")
@Where(clause = "deleted = 0")
public class KnowledgeBaseEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "milvus_collection", length = 200)
    private String milvusCollection;

    @Column(name = "chunk_size")
    private Integer chunkSize = 512;

    @Column(name = "chunk_overlap")
    private Integer chunkOverlap = 64;

    @Column(name = "doc_count")
    private Integer docCount = 0;

    private Integer status = 1;
}
