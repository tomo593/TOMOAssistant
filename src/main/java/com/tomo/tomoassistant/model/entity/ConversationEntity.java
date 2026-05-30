package com.tomo.tomoassistant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation", indexes = {
        @Index(name = "idx_conv_kb_id", columnList = "knowledge_base_id")
})
@EqualsAndHashCode(callSuper = true)
@SQLDelete(sql = "UPDATE conversation SET deleted = 1 WHERE id = ?")
@Where(clause = "deleted = 0")
public class ConversationEntity extends BaseEntity {

    @Column(length = 200)
    private String title;

    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    private Integer status = 1;
}
