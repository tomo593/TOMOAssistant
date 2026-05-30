package com.tomo.tomoassistant.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {
    private Long id;
    private String title;
    private Long knowledgeBaseId;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
