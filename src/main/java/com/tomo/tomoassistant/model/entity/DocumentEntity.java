package com.tomo.tomoassistant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@Table(name = "document", indexes = {
        @Index(name = "idx_doc_kb_id", columnList = "knowledge_base_id"),
        @Index(name = "idx_doc_status", columnList = "status")
})
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE document SET deleted = 1 WHERE id = ?")
@Where(clause = "deleted = 0")
public class DocumentEntity extends BaseEntity {

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "minio_key", length = 500)
    private String minioKey;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    private Integer status = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "image_description", columnDefinition = "TEXT")
    private String imageDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id", insertable = false, updatable = false)
    private KnowledgeBaseEntity knowledgeBase;
}
