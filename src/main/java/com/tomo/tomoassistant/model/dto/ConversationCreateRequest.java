package com.tomo.tomoassistant.model.dto;

import lombok.Data;

@Data
public class ConversationCreateRequest {
    private String title;
    private Long knowledgeBaseId;
    private String modelName;
    private String systemPrompt;
}
